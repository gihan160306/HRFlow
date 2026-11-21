#!/usr/bin/env python3
"""Sinh các sơ đồ ERD (PlantUML) từ docs/database_design.md.
Chạy:  python docs/uml/gen_erd.py   -> ghi docs/uml/erd/fig4_3..fig4_6_*.puml
"""
import re, pathlib, sys
HERE = pathlib.Path(__file__).resolve().parent
SRC = HERE.parent / "database_design.md"
text = SRC.read_text(encoding="utf-8")

GROUPS = {}   # nhóm -> [bảng]
cur = None
for line in text.split("\n"):
    m = re.match(r"### ([A-H])\. (.+?) \(", line)
    if m: cur = f"{m.group(1)}. {m.group(2)}"; GROUPS[cur] = []; continue
    if re.match(r"## 8\.", line): cur = None
    m = re.match(r"#### `(\w+)`", line)
    if m and cur: GROUPS[cur].append(m.group(1))

TABLES = {}
for m in re.finditer(r"#### `(\w+)`\n(.*?)(?=\n#### |\n### |\n## )", text, re.S):
    cols = []
    for c in re.finditer(r"^\| `(\w+)` \| ([^|]+)\|([^|]*)\|([^|]*)\|([^|]*)\|", m.group(2), re.M):
        name, typ, nn, _, desc = [x.strip() for x in c.groups()]
        fk = re.search(r"FK→\s*`(\w+)`", desc)
        cols.append(dict(name=name, type=typ, pk="PK" in desc.split("·")[0] and desc.startswith("PK"),
                         fk=fk.group(1) if fk else None, nn=bool(nn)))
    TABLES[m.group(1)] = cols

SHOW = {  # cột bổ sung cần hiển thị (ngoài PK/FK); None = 5 cột đầu
    "payroll_period": ["period_year", "period_month", "status", "payment_status", "snapshot_version", "calc_version"],
    "payroll_line": ["status", "gross", "insurance_total", "pit", "net_entitlement", "payable"],
    "payroll_line_item": ["category", "name", "amount"],
    "timesheet_snapshot": ["snapshot_version", "data"],
    "payslip": ["file_name", "generated_at", "published_at"],
    "payroll_audit": ["action", "from_status", "to_status", "created_at"],
    "payroll_adjustment": ["adj_type", "amount", "status"],
    "employee": ["code", "full_name", "employee_type", "status", "hire_date"],
    "user_account": ["email", "role", "status", "token_version"],
    "payroll_grant": ["grant_type", "effective_from", "effective_to"],
    "contract": ["contract_no", "contract_type", "start_date", "end_date", "status"],
    "salary_record": ["base_salary", "effective_from", "effective_to"],
    "tax_profile": ["residency", "method", "status", "effective_from"],
    "insurance_profile": ["insurance_salary", "status", "effective_from"],
    "attendance_record": ["work_date", "check_in_at", "check_out_at", "worked_minutes", "status"],
    "leave_request": ["start_date", "end_date", "duration_days", "status"],
    "ot_registration": ["registered_minutes", "approved_minutes", "status", "actual_minutes"],
    "ot_schedule": ["work_date", "start_time", "end_time", "status"],
    "employee_qr_token": ["nonce", "expires_at", "scanned_at", "punch_type", "used_at"],
    "attendance_point": ["code", "name", "active"],
    "user_account": ["email", "role", "status", "token_version"],
    "leave_balance": ["balance_year", "used_days"],
    "leave_request_day": ["leave_date", "minutes"],
    "department": ["code", "name"], "job_title": ["code", "name"],
}

def entity(t, show_cols=True):
    cols = TABLES.get(t, [])
    lines = [f"entity {t} {{"]
    pk = [c for c in cols if c["name"] == "id"]
    for c in pk: lines.append(f"  * id : {c['type']} <<PK>>")
    lines.append("  --")
    for c in cols:
        if c["fk"] and c["name"] != "id" and not c["name"].endswith(("_by", "_account_id")) :
            lines.append(f"  {'*' if c['nn'] else ''} {c['name']} : {c['type']} <<FK>>".replace("  * ", "  * ").replace("   ", "  "))
    extra = SHOW.get(t) or [c["name"] for c in cols if c["name"] != "id" and not c["fk"]][:5]
    shown = [c for c in cols if c["name"] in extra]
    if shown:
        lines.append("  --")
        for c in shown: lines.append(f"  {c['name']} : {c['type']}")
    lines.append("}")
    return "\n".join(lines)

def relations(tabs, label=False, skip=()):
    out = []
    s = set(tabs)
    for t in tabs:
        for c in TABLES.get(t, []):
            if c["fk"] and c["fk"] in s and c["name"] != "id" and c["fk"] not in skip and not c["name"].endswith("_by"):
                out.append(f"{c['fk']} ||--o{{ {t}" + (f" : {c['name']}" if label else ""))
    return out

def write(name, title, tabs, extra=""):
    body = ["@startuml " + name, "!include _style.puml", "hide circle", "skinparam linetype ortho",
            "skinparam entityBackgroundColor #F5FAFF", "skinparam entityBorderColor #1565C0",
            "skinparam defaultFontSize 15"]
    body += [entity(t) for t in tabs]
    body += relations(tabs, label=True)
    if extra: body.append(extra)
    body.append("@enduml")
    (HERE / "erd" / f"{name}.puml").write_text("\n".join(body) + "\n", encoding="utf-8")
    print("ghi", name, len(tabs), "bảng")

def overview():
    allt = [t for g in GROUPS.values() for t in g]
    body = ["@startuml fig4_3_erd_overview", "!include _style.puml", "hide circle", "hide empty members",
            "skinparam entityBackgroundColor #FFFFFF", "left to right direction", "skinparam nodesep 12", "skinparam ranksep 40", "skinparam entityBorderColor #1565C0"]
    colors = ["#E3F2FD", "#E8F5E9", "#FFF3E0", "#F3E5F5", "#FFFDE7", "#E0F7FA", "#FCE4EC", "#ECEFF1"]
    for (g, ts), col in zip(GROUPS.items(), colors):
        body.append(f'package "Nhóm {g[0]} – {g[3:]} ({len(ts)})" {col} {{')
        body += [f"  entity {t}" for t in ts]
        body.append("}")
    firsts = [ts[0] for ts in GROUPS.values()]
    body += [f"{firsts[i]} -[hidden]-> {firsts[i+1]}" for i in (0, 1, 2, 4, 5, 6)]
    body += [f"{firsts[i]} -[hidden]down-> {firsts[i+4]}" for i in range(4)]
    body += relations(allt, skip=("employee", "user_account", "department", "job_title"))
    body.append("@enduml")
    # quan hệ máy chấm công (KIOSK) – điểm chấm công cắt ngang nhóm A/C: không cho ảnh hưởng bố cục
    body = [l.replace("attendance_point ||--o{ user_account", "attendance_point ||-[norank]-o{ user_account") for l in body]
    (HERE / "erd" / "fig4_3_erd_overview.puml").write_text("\n".join(body) + "\n", encoding="utf-8")
    print("ghi overview", len(allt), "bảng")

if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    overview()
    write("fig4_4_erd_account_hr", "ERD nhóm tài khoản, tổ chức và hồ sơ nhân sự",
          "user_account payroll_grant employee department job_title contract salary_record tax_profile insurance_profile".split())
    write("fig4_5_erd_attendance_leave_ot", "ERD nhóm chấm công, nghỉ phép và làm thêm giờ",
          "employee user_account attendance_point employee_qr_token attendance_record leave_request leave_request_day leave_balance ot_schedule ot_registration".split())
    write("fig4_6_erd_payroll", "ERD nhóm kỳ lương và phiếu lương",
          "employee payroll_period timesheet_snapshot payroll_line payroll_line_item payroll_adjustment payslip payroll_audit".split())
