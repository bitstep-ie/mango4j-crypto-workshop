Why the Naive Approach Is the Most Common One

## Why people do it

**It's the shortest path to working code.** The first requirement is almost always "this column needs to be encrypted". The shortest answer is `encrypt(plaintext)` and `store(ciphertext)`. Nothing in that moment asks "which key?", because there is only one key, and one set of encryption parameters. The metadata problem doesn't exist yet, as there is only one encryption mechanism, so it doesn't get designed for.

**The problems are invisible until much later.** Key rotation, provider changes and retiring a key are all future events. A team shipping a feature this sprint is rewarded for working code now, and nobody is penalised in the demo for not predicting the future. The failure modes only appear after a rotation, an audit finding or a compliance deadline, by then the data is already in production for quite some time.

**Libraries and tutorials teach it.** Cipher API examples, Stack Overflow answers and blog posts show `Cipher.getInstance(...)`, `init`, `doFinal`, and stop. They treat the output bytes as "the ciphertext". Most developers copy that shape, and the IV is the only extra piece they learn to carry along.

**A single key hides the design flaw.** With one key and one provider, the naive design works and passes every test. Single-key systems never expose the missing elements, like key ID, so there is no feedback telling the team to change course.

**The structured approach needs foresight.** It asks the team to build a key object, a resolver and a self-describing format before there is a concrete need. That looks like over-engineering to anyone who hasn't been through a painful rotation. The people who do it right have usually done it wrong before.

**Retrofitting is expensive, so it keeps getting deferred.** Once naive blobs exist, moving to a structured format means a migration of live encrypted data. That is exactly the work the structured format would have avoided. So the naive design tends to stay until outages for key rotation become too long to be acceptable.

**Ownership is split.** The developer writing the feature rarely owns key management, compliance or operations. The people who feel the rotation pain are not the people who chose the storage format.

