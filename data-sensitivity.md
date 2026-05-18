### NRIC Masking

- Format: first characters + `****` + last character
- Example: `T2400001A` → `T240****A`
- Apply to **all** NRIC fields in **all** responses — both `childNric` and `parentNric`
- Apply in audit logs too — never log a raw NRIC