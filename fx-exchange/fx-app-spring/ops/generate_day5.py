#!/usr/bin/env python3
"""Generates transactions.csv (seed 42) + solution/answers-day5.md with expected results.

REBUILT 2026-07-16 for the full SQL alignment: the CSV now carries the exact
columns of fxdb's `transfer` table (id, from_account, to_account, amount,
currency_code, executed_at, status). 1000 distinct rows + 5 logical duplicates
(same everything except id) + 3 PLANTED MALFORMED rows the loader must skip:
a letter-O amount, a truncated row, and an unknown currency code.
"""
import random, os
from collections import defaultdict
random.seed(42)
HERE = os.path.dirname(os.path.abspath(__file__)); SOL = os.path.join(HERE, "..", "..", "solution")
CURRENCIES = ["USD", "EUR", "GBP", "JPY", "CHF", "AUD", "CAD", "SGD"]   # = the currency table
STATUSES = ["COMPLETED"] * 78 + ["PENDING"] * 12 + ["FAILED"] * 10       # weighted like the seed SQL

rows = []
for i in range(1, 1001):
    frm = random.randint(1, 20)                       # fxdb has accounts 1..20
    to = random.randint(1, 20)
    while to == frm:
        to = random.randint(1, 20)
    amt = round(random.uniform(50, 20000), 2)
    ccy = random.choice(CURRENCIES)
    ts = f"2026-01-{random.randint(10,12):02d}T{random.randint(8,17):02d}:{random.randint(0,59):02d}:00"
    rows.append((i, frm, to, amt, ccy, ts, random.choice(STATUSES)))

# 5 exact duplicates (same everything except id) for the Set/dedupe task —
# drawn from safely-below-maximum amounts so the top-1 row stays unambiguous
candidates = [r for r in rows if r[3] < 19000]
for k in range(5):
    src = candidates[random.randint(0, len(candidates) - 1)]
    rows.append((1001 + k,) + src[1:])
random.shuffle(rows)

lines = [",".join(map(str, r)) for r in rows]
# 3 planted malformed rows — the robust-loader task skips these, chaining the cause
malformed = [
    "9001,4,11,127O4.55,EUR,2026-01-11T09:12:00,COMPLETED",   # letter O in the amount
    "9002,7,3,5120.40,GBP,2026-01-12T15:47:00",                # truncated: status missing
    "9003,12,9,8033.19,XXL,2026-01-10T11:05:00,PENDING",       # no such currency
]
for bad in malformed:
    lines.insert(random.randint(0, len(lines)), bad)

with open(os.path.join(HERE, "transactions.csv"), "w") as f:
    f.write("id,from_account,to_account,amount,currency_code,executed_at,status\n")
    f.write("\n".join(lines) + "\n")

# ── ground truth ─────────────────────────────────────────────────────────────
seen = {}
dup = 0
for r in rows:                              # id excluded from the logical key
    key = r[1:]
    if key in seen: dup += 1
    else: seen[key] = r
deduped = list(seen.values())

by_ccy = defaultdict(int)
for r in deduped: by_ccy[r[4]] += 1
busiest = max(by_ccy.items(), key=lambda kv: (kv[1], kv[0]))

top = max(rows, key=lambda r: r[3])
purged = [r for r in deduped if r[3] >= 100.00]
failed = [r for r in purged if r[6] == "FAILED"]
totals = defaultdict(float); counts = defaultdict(int)
for r in deduped:
    totals[r[6]] += r[3]; counts[r[6]] += 1

with open(os.path.join(SOL, "answers-day5.md"), "w") as f:
    f.write("# Day 5 — computed ground truth (seed 42, DB-shaped transfers)\n\n")
    f.write(f"- data lines in the file: {len(lines)} (incl. 3 malformed to skip)\n")
    f.write(f"- loaded after skipping malformed: {len(rows)}\n")
    f.write(f"- logical duplicates (same everything except id): {dup}\n")
    f.write(f"- distinct after dedupe: {len(deduped)}\n\n")
    f.write("## Busiest currency (count on the deduped list)\n\n")
    for c, n in sorted(by_ccy.items(), key=lambda kv: -kv[1]):
        f.write(f"- {c}: {n}\n")
    f.write(f"\n→ busiest: **{busiest[0]} ({busiest[1]})**\n")
    f.write(f"\n## Largest single transfer (full list — duplicates can't out-earn it)\n\n")
    f.write(f"- id={top[0]} {top[1]}->{top[2]} amount={top[3]:.2f} {top[4]} {top[5]} {top[6]}\n")
    f.write(f"\n## After removeIf(amount < 100) on the deduped list\n\n- {len(purged)}\n")
    f.write(f"\n## FAILED transfers surviving the purge (method-ref filter)\n\n- {len(failed)}\n")
    f.write("\n## Totals per status (deduped list — Task 3's pool must reproduce these)\n\n")
    for s in sorted(totals):
        f.write(f"- {s}: {totals[s]:.2f} ({counts[s]} transfers)\n")
print("day5:", len(lines), "data lines,", len(rows), "loadable,", dup, "dupes,",
      len(deduped), "distinct, busiest", busiest, "| top id", top[0], f"{top[3]:.2f}",
      "| purged", len(purged), "| failed", len(failed))
for s in sorted(totals):
    print(f"  {s}: {totals[s]:.2f} ({counts[s]})")
