"""Print a portable SHA-256 checksum for a release asset."""
import hashlib
import pathlib
import sys
p = pathlib.Path(sys.argv[1])
print(f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}")
