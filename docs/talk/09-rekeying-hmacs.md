# 9. Rekeying: HMACs

*Content coming soon.*

- The HMAC half of a rekey operation: regenerating each row's HMAC(s) under the new key
- Why this is split out from the encryption rekey (Chapter 8) as its own step
- How this interacts with the list HMAC strategy (Chapter 7) — old HMACs can be dropped from the list once the rekey completes and rotation is done

## Closing: what "doing it right" actually requires

- Decoupling application code from cryptographic providers
- Understanding the unencrypted → encrypted migration path up front, before you need it

## What we're building today

- The hands-on stages, in order
- What each stage demonstrates from this talk
