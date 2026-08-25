# 9. Rekeying: HMACs

*Content coming soon.*

- The HMAC half of a rekey operation: regenerating each row's HMAC(s) under the new key
- Why this is split out from the encryption rekey (Chapter 8) as its own step
- How this interacts with the list HMAC strategy (Chapter 7) — old HMACs can be dropped from the list once the rekey completes and rotation is done
