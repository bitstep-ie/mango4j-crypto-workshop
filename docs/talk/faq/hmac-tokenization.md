What is HMAC tokenization / derived-value search?

Alongside the HMAC of a full value, you can also store HMACs of *derived* representations of it — the last four digits of a card number, a normalized form with punctuation stripped — enabling partial-match search without ever weakening or exposing the underlying encrypted value. See [List HMAC Strategy](../list-hmac.md).
