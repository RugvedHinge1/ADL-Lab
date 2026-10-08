"""Print table names and row counts for the pulled Scholr database.

Usage: python scripts/inspect-db.py [path/to/scholr.db]
Defaults to local-db/scholr.db (as produced by scripts/pull-db.ps1).
"""
import sqlite3
import sys
from pathlib import Path

db_path = sys.argv[1] if len(sys.argv) > 1 else str(Path(__file__).parent.parent / "local-db" / "scholr.db")

conn = sqlite3.connect(db_path)
cur = conn.cursor()
cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
tables = [r[0] for r in cur.fetchall()]
print(f"Database: {db_path}")
print("Tables:")
for t in tables:
    cur.execute(f"SELECT COUNT(*) FROM '{t}'")
    count = cur.fetchone()[0]
    print(f"  {t}: {count} rows")
conn.close()
