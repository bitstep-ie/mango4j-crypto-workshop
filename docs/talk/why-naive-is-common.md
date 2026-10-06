Why the Naive Approach Is the Most Common One

Most teams end up with the naive approach (encrypt the value, store the ciphertext) because every incentive on day one points toward it, and the costs only show up much later. See [Structured Ciphertext](structured-ciphertext.md) for what it looks like and why it breaks down.

## Why people take it

**It's the shortest path to working code.** The first requirement is almost always "encrypt this column". The shortest answer is `encrypt(plaintext)` and `store(ciphertext)`. Nothing in that moment asks "which key?", because there is only one key. The metadata problem doesn't exist yet, so it doesn't get designed for.

**The problems are invisible until much later.** Key rotation, provider changes and retiring a key are all future events. A team shipping a feature this sprint is rewarded for working code now, and nobody is penalised in the demo for missing metadata. The failure modes (the brute-force decrypt, the unanswerable "which records still use this key?") only appear after a rotation, an audit finding or a compliance deadline. By then the data is already in production.

**Libraries and tutorials teach it.** Cipher API examples, Stack Overflow answers and blog posts show `Cipher.getInstance(...)`, `init`, `doFinal`, and stop. They treat the output bytes as "the ciphertext". Most developers copy that shape, and the IV is the only extra piece they learn to carry along.

**A single key hides the design flaw.** With one key and one provider, the naive design works and passes every test. Single-key systems never expose the missing key ID, so there is no feedback telling the team to change course.

**The structured approach needs foresight.** It asks the team to build a key object, a resolver and a self-describing format before there is a concrete need. That looks like over-engineering to anyone who hasn't been through a painful rotation. The people who do it right have usually been burned before.

**Retrofitting is expensive, so it keeps getting deferred.** Once naive blobs exist, moving to a structured format means a migration of live encrypted data. That is exactly the work the structured format would have avoided. So the naive design tends to stay.

**Ownership is split.** The developer writing the feature rarely owns key management, compliance or operations. The people who feel the rotation pain are not the people who chose the storage format.

## Why the talk starts here

This is why the talk leads with the naive version and runs it until it fails. It lets the audience see why the extra structure matters before the structured version is introduced.
