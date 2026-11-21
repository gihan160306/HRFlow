"""Kiểm thử API chấm công tại quầy (SRS FR-07, DEC-10; PM-21..PM-23).

Chạy với backend đang chạy trên CSDL có dữ liệu demo V20–V22, mỗi nhân viên nv009–nv011 chưa chấm công hôm nay:
    python test/kiosk_attendance_api_test.py            # mặc định http://localhost:8080
    HRFLOW_API=http://localhost:18080/api/v1 python test/kiosk_attendance_api_test.py
Script ghi dữ liệu thật (bản ghi công, tài khoản KIOSK mới) – chỉ chạy trên CSDL thử nghiệm.
"""
import os
import json, time, urllib.request, urllib.error, base64, sys
sys.stdout.reconfigure(encoding='utf-8')
BASE = os.environ.get('HRFLOW_API', 'http://localhost:8080/api/v1')
PW = 'password123'
# 1x1 JPEG
JPEG = base64.b64encode(bytes.fromhex(
    'ffd8ffe000104a46494600010100000100010000ffdb004300080606070605080707070909080a0c140d0c0b0b0c1912130f141d1a1f1e1d1a1c1c20242e2720222c231c1c2837292c30313434341f27393d38323c2e333432ffc0000b080001000101011100ffc4001f0000010501010101010100000000000000000102030405060708090a0bffc400b5100002010303020403050504040000017d01020300041105122131410613516107227114328191a1082342b1c11552d1f02433627282090a161718191a25262728292a3435363738393a434445464748494a535455565758595a636465666768696a737475767778797a838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae1e2e3e4e5e6e7e8e9eaf1f2f3f4f5f6f7f8f9faffda0008010100003f00fbd3ffd9')).decode()

results = []
KIOSK2 = 'kiosk.test%d@hnqmedia.example' % int(time.time())
def call(method, path, token=None, body=None):
    req = urllib.request.Request(BASE + path, method=method,
                                 data=None if body is None else json.dumps(body).encode(),
                                 headers={'Content-Type': 'application/json', **({'Authorization': 'Bearer ' + token} if token else {})})
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, (json.loads(raw) if raw and r.headers.get_content_type() == 'application/json' else raw)
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw[:200]

def check(name, cond, info=''):
    results.append((name, cond))
    print(('PASS ' if cond else 'FAIL ') + name + ('' if cond else '  -> ' + str(info)[:300]))

def login(email):
    s, b = call('POST', '/auth/login', body={'email': email, 'password': PW})
    assert s == 200, (email, s, b)
    return b['token'], b

emp, _ = login('nv009@hnqmedia.example')
emp2, _ = login('nv010@hnqmedia.example')
emp3, _ = login('nv011@hnqmedia.example')
hr, _ = login('nv004@hnqmedia.example')
admin, _ = login('admin@hnqmedia.example')
kiosk, kb = login('kiosk.letan@hnqmedia.example')

check('KIOSK login: role KIOSK, gắn điểm quầy', kb['user']['role'] == 'KIOSK' and kb['user']['attendancePointName'], kb['user'])
from datetime import datetime, timezone
exp = datetime.fromisoformat(kb['expiresAt'].replace('Z', '+00:00'))
hours = (exp - datetime.now(timezone.utc)).total_seconds() / 3600
check('KIOSK token ~12 giờ', 11.5 < hours <= 12.01, hours)
s, b = call('GET', '/kiosk/me', kiosk)
check('GET /kiosk/me', s == 200 and b['pointCode'] == 'RECEPTION', (s, b))

# PM-21 / PM-22
s, _ = call('GET', '/attendance/today', kiosk); check('PM-21 KIOSK gọi /attendance/today -> 403', s == 403, s)
s, _ = call('GET', '/employees', kiosk); check('PM-21 KIOSK gọi /employees -> 403', s == 403, s)
s, _ = call('GET', '/attendance/my-qr', kiosk); check('PM-21 KIOSK gọi /attendance/my-qr -> 403', s == 403, s)
s, _ = call('POST', '/kiosk/scan', emp, {'qrPayload': 'x'}); check('PM-22 Employee gọi /kiosk/scan -> 403', s == 403, s)
s, _ = call('POST', '/kiosk/scan', admin, {'qrPayload': 'x'}); check('PM-22 Admin gọi /kiosk/scan -> 403', s == 403, s)
s, _ = call('GET', '/attendance/my-qr', admin); check('Admin gọi /attendance/my-qr -> 403', s == 403, s)
s, _ = call('POST', '/kiosk/scan', None, {'qrPayload': 'x'}); check('Không token gọi /kiosk/scan -> 401', s == 401, s)

# Old endpoints removed
s, _ = call('POST', '/attendance/check-in', emp, {}); check('API cũ /attendance/check-in không còn', s in (404, 405), s)
s, _ = call('GET', '/attendance/qr?pointId=1', hr); check('API cũ /attendance/qr không còn', s in (404, 405), s)

# QR invalid
s, b = call('POST', '/kiosk/scan', kiosk, {'qrPayload': 'HRFLOW-QR:abc'}); check('PM-23 QR sai định dạng -> 422 QR_INVALID', s == 422 and b['code'] == 'QR_INVALID', (s, b))
s, b = call('POST', '/kiosk/scan', kiosk, {'qrPayload': 'HRFLOW-EMP:khongtontai'}); check('PM-23 QR không do server cấp -> 422 QR_INVALID', s == 422 and b['code'] == 'QR_INVALID', (s, b))

# Happy path check-in
s, q = call('GET', '/attendance/my-qr', emp)
check('Employee lấy QR cá nhân, hạn 15 giây', s == 200 and q['qrPayload'].startswith('HRFLOW-EMP:') and q['ttlSeconds'] == 15 and 'NV' not in q['qrPayload'], (s, q))
s, sc = call('POST', '/kiosk/scan', kiosk, {'qrPayload': q['qrPayload']})
check('Quét QR -> 200 CHECK_IN, chưa ghi công', s == 200 and sc['action'] == 'CHECK_IN', (s, sc))
s, t = call('GET', '/attendance/today', emp); check('Sau quét chưa có bản ghi công', s == 204, (s, t))
s, b = call('POST', '/kiosk/scan', kiosk, {'qrPayload': q['qrPayload']}); check('Quét lại cùng mã -> 422 QR_USED', s == 422 and b['code'] == 'QR_USED', (s, b))
s, b = call('POST', '/kiosk/punch', kiosk, {'scanId': sc['scanId']}); check('Gửi thiếu ảnh -> 422 PHOTO_REQUIRED', s == 422 and b['code'] == 'PHOTO_REQUIRED', (s, b))
s, b = call('POST', '/kiosk/punch', kiosk, {'scanId': sc['scanId'], 'photoBase64': 'A' * 300000, 'photoMime': 'image/jpeg'}); check('Ảnh quá 200 KB -> 422 PHOTO_TOO_LARGE', s == 422 and b['code'] == 'PHOTO_TOO_LARGE', (s, b))
s, p = call('POST', '/kiosk/punch', kiosk, {'scanId': sc['scanId'], 'photoBase64': 'data:image/jpeg;base64,' + JPEG, 'photoMime': 'image/jpeg'})
check('Gửi ảnh -> 200 ghi check-in', s == 200 and p['action'] == 'CHECK_IN' and p['pointName'], (s, p))
s, b = call('POST', '/kiosk/punch', kiosk, {'scanId': sc['scanId'], 'photoBase64': JPEG}); check('Gửi lại phiên đã dùng -> 422 QR_USED', s == 422 and b['code'] == 'QR_USED', (s, b))
s, t = call('GET', '/attendance/today', emp)
check('Bản ghi hôm nay có giờ vào, điểm, máy quầy, ảnh', s == 200 and t['checkInAt'] and t['checkInPoint'] and t['checkInKiosk'] == 'kiosk.letan@hnqmedia.example' and t['hasCheckInPhoto'] and t['method'] == 'QR', (s, t))
rec_id = t['id'] if s == 200 else None
s, b = call('GET', f'/attendance/{rec_id}/photo?kind=in', emp); check('Nhân viên xem ảnh của mình', s == 200, s)
s, b = call('GET', f'/attendance/{rec_id}/photo?kind=in', emp2); check('Nhân viên khác không xem được ảnh', s in (403, 404), s)
s, b = call('GET', f'/attendance/{rec_id}/photo?kind=in', admin); check('Admin không xem được ảnh', s in (403, 404), s)

# Too soon to check out
s, q2 = call('GET', '/attendance/my-qr', emp)
s, b = call('POST', '/kiosk/scan', kiosk, {'qrPayload': q2['qrPayload']})
check('Quét lại ngay sau check-in -> 409 TOO_SOON_TO_CHECK_OUT', s == 409 and b['code'] == 'TOO_SOON_TO_CHECK_OUT' and 'check-in lúc' in b['message'], (s, b))

# Revoked by newer code
s, a = call('GET', '/attendance/my-qr', emp2)
s, bb = call('GET', '/attendance/my-qr', emp2)
s, b = call('POST', '/kiosk/scan', kiosk, {'qrPayload': a['qrPayload']}); check('Mã cũ bị thay bằng mã mới -> 422 QR_EXPIRED', s == 422 and b['code'] == 'QR_EXPIRED', (s, b))

# Expired
s, e3 = call('GET', '/attendance/my-qr', emp3)
time.sleep(16)
s, b = call('POST', '/kiosk/scan', kiosk, {'qrPayload': e3['qrPayload']}); check('PM-23 QR quá 15 giây -> 422 QR_EXPIRED', s == 422 and b['code'] == 'QR_EXPIRED', (s, b))

# Audit of rejects
s, logs = call('GET', '/audit-logs?action=KIOSK_SCAN_REJECTED', admin)
print('   audit-logs status', s)

# Points & accounts
s, pts = call('GET', '/attendance-points', hr)
check('HR xem điểm chấm công có kioskCount, không còn photoRequired', s == 200 and pts[0]['kioskCount'] == 1 and 'photoRequired' not in pts[0], (s, pts))
s, b = call('POST', '/accounts', admin, {'email': KIOSK2, 'role': 'KIOSK', 'reason': 'Máy quầy tầng 2', 'temporaryPassword': 'Kiosk@12345'})
check('Tạo KIOSK thiếu điểm -> 422', s == 422 and b['code'] == 'ATTENDANCE_POINT_REQUIRED', (s, b))
s, b = call('POST', '/accounts', admin, {'email': KIOSK2, 'role': 'KIOSK', 'employeeId': 9, 'attendancePointId': 1, 'reason': 'x', 'temporaryPassword': 'Kiosk@12345'})
check('Tạo KIOSK gắn nhân viên -> 422', s == 422, (s, b))
s, b = call('POST', '/accounts', admin, {'email': KIOSK2, 'role': 'KIOSK', 'attendancePointId': 1, 'reason': 'Máy quầy tầng 2', 'temporaryPassword': 'Kiosk@12345'})
check('Admin tạo tài khoản KIOSK gắn điểm', s == 201 and b['account']['role'] == 'KIOSK' and b['account']['attendancePointId'] == 1, (s, b))
new_id = b['account']['id'] if s == 201 else None
s, b = call('PATCH', f'/accounts/{new_id}/role', admin, {'role': 'EMPLOYEE', 'reason': 'x'})
if s in (404, 405):
    s, b = call('PUT', f'/accounts/{new_id}/role', admin, {'role': 'EMPLOYEE', 'reason': 'x'})
check('Không đổi KIOSK thành role khác -> 422', s == 422 and b.get('code') == 'KIOSK_ROLE_FIXED', (s, b))
s, b = call('POST', '/accounts', hr, {'email': 'x@hnqmedia.example', 'role': 'KIOSK', 'attendancePointId': 1, 'reason': 'x'})
check('HR không tự tạo được tài khoản KIOSK -> 403', s == 403, (s, b))

# Lock kiosk -> token revoked
s, b = call('POST', f'/accounts/{new_id}/lock', admin, {'reason': 'test'})
print('   lock', s)

print()
ok = sum(1 for _, c in results if c)
print('TỔNG:', ok, '/', len(results), 'PASS')
sys.exit(0 if ok == len(results) else 1)
