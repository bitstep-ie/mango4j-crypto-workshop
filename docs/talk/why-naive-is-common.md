Why the Naive Approach Is the Most Common One

## Why developers do it

**It's the quickest way to working code.** The first request is usually "this column needs to be encrypted". So you call `encrypt()` and store the result. There's only one key at the start, so nobody asks "which key?" or "what happens when that key has to change".

**The problems show up later.** Rotating keys, switching providers and retiring keys all happen in the future. Teams get rewarded for shipping now, and nobody is marked down in a demo for failing to predict the future. By the time an audit or a rotation exposes the gap, the data has been in production for a long time.

**Tutorials teach it.** Most examples show `Cipher.getInstance(...)`, `init` and `doFinal`, then stop. They treat the output bytes as "the ciphertext". Developers copy that, and the IV is the only extra piece they learn to store.

**One key hides the flaw.** With a single key and a single provider, the naive design passes every test. Nothing is missing until there is more than one key, so nothing tells the team to change course.

**The better design needs foresight.** It means building a key object, a resolver and a self-describing format before you need them. That looks like over-engineering to anyone who hasn't been through a painful rotation. The people who get it right have usually got it wrong before.

**Fixing it later is expensive.** Once naive blobs are in the database, moving to a structured format means migrating live encrypted data, which is the work the structured format would have avoided. So the migration keeps getting put off, often until rotation outages become unacceptably long.

**Nobody owns the whole problem.** The developer who writes the feature is rarely the one who has to rotate the keys later.
