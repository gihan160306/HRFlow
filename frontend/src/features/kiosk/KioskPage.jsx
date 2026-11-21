import React, { useCallback, useEffect, useRef, useState } from 'react';
import jsQR from 'jsqr';
import { Camera, CheckCircle2, LogOut, ScanLine, XCircle } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import kioskApi from '../../api/kioskApi';
import { formatTime } from '../../utils/formatDate';

/*
 * Máy chấm công tại quầy (SRS FR-07, DEC-10) – chỉ tài khoản thiết bị KIOSK.
 * SCANNING → VERIFYING → COUNTDOWN (chào nhân viên) → UPLOADING → SUCCESS/ERROR → quay lại SCANNING.
 * Camera chỉ mở được trên HTTPS hoặc localhost (giới hạn của trình duyệt).
 */
const SCAN_INTERVAL_MS = 200;
const COUNTDOWN_S = 3;
const RESULT_MS = 3500;
const ERROR_MS = 3000;
const MAX_WIDTH = 640;

function useClock() {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const t = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(t);
  }, []);
  return now;
}

/** Chụp một khung hình từ video, nén JPEG cho vừa giới hạn dung lượng (KB). */
function captureJpeg(video, maxKb) {
  const scale = Math.min(1, MAX_WIDTH / (video.videoWidth || MAX_WIDTH));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round((video.videoWidth || MAX_WIDTH) * scale);
  canvas.height = Math.round((video.videoHeight || 480) * scale);
  canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
  const limit = (maxKb || 200) * 1024;
  let quality = 0.8;
  let dataUrl = canvas.toDataURL('image/jpeg', quality);
  // base64 dài hơn dữ liệu thật ~4/3
  while (dataUrl.length * 0.75 > limit && quality > 0.3) {
    quality -= 0.1;
    dataUrl = canvas.toDataURL('image/jpeg', quality);
  }
  return dataUrl;
}

export function KioskPage() {
  const { logout } = useAuth();
  const now = useClock();
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const busy = useRef(false);
  // Mã QR chỉ dùng một lần: mã đã được server trả lời (thành công hay lỗi) thì không gửi lại,
  // tránh trường hợp nhân viên vẫn để điện thoại trước camera sau khi chấm xong.
  const seenCodes = useRef(new Set());

  const [info, setInfo] = useState(null);
  const [cameraError, setCameraError] = useState('');
  const [phase, setPhase] = useState('SCANNING'); // SCANNING | VERIFYING | COUNTDOWN | UPLOADING | SUCCESS | ERROR
  const [scan, setScan] = useState(null);
  const [count, setCount] = useState(COUNTDOWN_S);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [snapshot, setSnapshot] = useState(null);

  useEffect(() => {
    kioskApi.me().then(setInfo).catch((e) => setError(e.message));
  }, []);

  // Mở camera
  useEffect(() => {
    let stream;
    const start = async () => {
      if (!navigator.mediaDevices?.getUserMedia) {
        setCameraError('Trình duyệt không cho dùng camera. Hãy mở trang qua HTTPS hoặc http://localhost.');
        return;
      }
      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 720 } }, audio: false });
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play().catch(() => {});
        }
      } catch (e) {
        setCameraError(e?.name === 'NotAllowedError'
          ? 'Chưa được cấp quyền camera. Hãy cho phép camera cho trang này rồi tải lại.'
          : `Không mở được camera (${e?.name || 'lỗi'}).`);
      }
    };
    start();
    return () => stream?.getTracks().forEach((t) => t.stop());
  }, []);

  const backToScan = useCallback((delay) => {
    setTimeout(() => {
      setPhase('SCANNING');
      setScan(null);
      setResult(null);
      setError('');
      setSnapshot(null);
      busy.current = false;
    }, delay);
  }, []);

  const fail = useCallback((message) => {
    setError(message);
    setPhase('ERROR');
    backToScan(ERROR_MS);
  }, [backToScan]);

  // Gửi mã vừa đọc được
  const onCode = useCallback(async (value) => {
    busy.current = true;
    setPhase('VERIFYING');
    try {
      const res = await kioskApi.scan(value);
      seenCodes.current.add(value);
      setScan(res);
      setCount(COUNTDOWN_S);
      setPhase('COUNTDOWN');
    } catch (e) {
      if (e.status) seenCodes.current.add(value); // lỗi mạng (status 0) thì cho quét lại
      fail(e.message);
    }
  }, [fail]);

  // Vòng quét QR
  useEffect(() => {
    if (phase !== 'SCANNING' || cameraError) return undefined;
    const t = setInterval(() => {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      if (busy.current || !video || !canvas || video.readyState < 2 || !video.videoWidth) return;
      const w = Math.min(MAX_WIDTH, video.videoWidth);
      const h = Math.round(video.videoHeight * (w / video.videoWidth));
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext('2d', { willReadFrequently: true });
      ctx.drawImage(video, 0, 0, w, h);
      const code = jsQR(ctx.getImageData(0, 0, w, h).data, w, h, { inversionAttempts: 'dontInvert' });
      if (!code?.data) return;
      if (!seenCodes.current.has(code.data)) onCode(code.data);
    }, SCAN_INTERVAL_MS);
    return () => clearInterval(t);
  }, [phase, cameraError, onCode]);

  // Đếm ngược rồi chụp ảnh và ghi công
  useEffect(() => {
    if (phase !== 'COUNTDOWN') return undefined;
    if (count <= 0) {
      const video = videoRef.current;
      const dataUrl = captureJpeg(video, scan?.photoMaxKb || info?.photoMaxKb);
      setSnapshot(dataUrl);
      setPhase('UPLOADING');
      kioskApi.punch(scan.scanId, dataUrl, 'image/jpeg')
        .then((res) => {
          setResult(res);
          setPhase('SUCCESS');
          backToScan(RESULT_MS);
        })
        .catch((e) => fail(e.message));
      return undefined;
    }
    const t = setTimeout(() => setCount((c) => c - 1), 1000);
    return () => clearTimeout(t);
  }, [phase, count, scan, info, backToScan, fail]);

  const actionLabel = (a) => (a === 'CHECK_OUT' ? 'Check-out' : 'Check-in');

  return (
    <div className="min-h-screen bg-slate-950 text-white flex flex-col">
      <header className="flex items-center justify-between px-6 py-4 border-b border-white/10">
        <div>
          <p className="text-xs uppercase tracking-widest text-slate-400">Máy chấm công tại quầy</p>
          <h1 className="text-lg font-bold">{info?.pointName || 'Đang tải điểm chấm công...'}</h1>
        </div>
        <div className="text-right">
          <p className="text-3xl font-mono font-extrabold tabular-nums">{now.toLocaleTimeString('vi-VN')}</p>
          <p className="text-xs text-slate-400">{now.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}</p>
        </div>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center gap-6 p-6">
        <div className="relative w-full max-w-3xl aspect-video rounded-3xl overflow-hidden bg-black border border-white/10">
          <video ref={videoRef} muted playsInline className="w-full h-full object-cover -scale-x-100" />
          <canvas ref={canvasRef} className="hidden" />

          {cameraError && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-slate-900/90 p-8 text-center">
              <Camera className="w-12 h-12 text-amber-400" />
              <p className="text-lg font-semibold">{cameraError}</p>
            </div>
          )}

          {!cameraError && phase === 'SCANNING' && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="w-64 h-64 border-4 border-emerald-400/80 rounded-3xl shadow-[0_0_0_9999px_rgba(0,0,0,0.35)]" />
            </div>
          )}

          {(phase === 'VERIFYING' || phase === 'UPLOADING') && (
            <div className="absolute inset-0 flex items-center justify-center bg-slate-900/70">
              <p className="text-2xl font-bold animate-pulse">{phase === 'VERIFYING' ? 'Đang kiểm tra mã...' : 'Đang ghi nhận chấm công...'}</p>
            </div>
          )}

          {phase === 'COUNTDOWN' && scan && (
            <div className="absolute inset-0 flex flex-col items-center justify-end pb-10 bg-gradient-to-t from-slate-900/90 via-transparent">
              <p className="text-3xl font-extrabold">Xin chào {scan.employeeName}</p>
              <p className="text-lg text-emerald-300 font-semibold mt-1">{actionLabel(scan.action)} – nhìn vào camera để chụp ảnh</p>
              <p className="text-7xl font-black mt-3 tabular-nums">{count}</p>
            </div>
          )}

          {phase === 'SUCCESS' && result && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-emerald-700/95 text-center p-6">
              <CheckCircle2 className="w-20 h-20" />
              <p className="text-3xl font-extrabold">{actionLabel(result.action)} thành công</p>
              <p className="text-xl">{result.employeeCode} – {result.employeeName}</p>
              <p className="text-5xl font-mono font-black">{formatTime(result.punchedAt)}</p>
              {result.lateMinutes > 0 && <p className="text-amber-200">Đi muộn {result.lateMinutes} phút</p>}
              {snapshot && <img src={snapshot} alt="Ảnh minh chứng vừa chụp" className="w-32 rounded-xl border-2 border-white/60 -scale-x-100" />}
            </div>
          )}

          {phase === 'ERROR' && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-red-700/95 text-center p-6">
              <XCircle className="w-20 h-20" />
              <p className="text-3xl font-extrabold">Không ghi nhận được</p>
              <p className="text-xl max-w-xl">{error}</p>
            </div>
          )}
        </div>

        <p className="flex items-center gap-2 text-lg text-slate-300">
          <ScanLine className="w-6 h-6 text-emerald-400" />
          Mở HRFlow trên điện thoại → bấm <b className="text-white">Chấm công</b> → đưa mã QR vào khung.
        </p>
      </main>

      <footer className="flex items-center justify-between px-6 py-3 text-xs text-slate-500 border-t border-white/10">
        <span>Ảnh chỉ dùng để đối chiếu chấm công, không nhận diện khuôn mặt. {info?.email}</span>
        <button type="button" onClick={logout} className="inline-flex items-center gap-1.5 hover:text-slate-200">
          <LogOut className="w-4 h-4" /> Đăng xuất máy quầy
        </button>
      </footer>
    </div>
  );
}

export default KioskPage;
