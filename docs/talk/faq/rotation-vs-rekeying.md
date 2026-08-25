What's the difference between key rotation and rekeying?

Rotation is switching which key gets used for *new* writes going forward — it doesn't touch anything already written. Rekeying is the separate background process that goes back and moves *existing* records off an old key onto the new one, which is what eventually lets the old key be deleted. Rotation is quick and low-risk; rekeying is the slower, higher-effort part. See [Key Rotation](../key-rotation.md), [Rekeying: Encryption](../rekeying-encryption.md), and [Rekeying: HMACs](../rekeying-hmacs.md).
