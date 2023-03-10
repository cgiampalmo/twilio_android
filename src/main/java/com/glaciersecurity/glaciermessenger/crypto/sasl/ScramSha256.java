package com.glaciersecurity.glaciermessenger.crypto.sasl;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;

import java.security.SecureRandom;

import com.glaciersecurity.glaciermessenger.entities.Account;
import com.glaciersecurity.glaciermessenger.xml.TagWriter;

public class ScramSha256 extends ScramMechanism {
	@Override
	protected HMac getHMAC() {
		return new HMac(new SHA256Digest());
	}

	@Override
	protected Digest getDigest() {
		return new SHA256Digest();
	}

	public ScramSha256(final TagWriter tagWriter, final Account account, final SecureRandom rng) {
		super(tagWriter, account, rng);
	}

	@Override
	public int getPriority() {
		return 25;
	}

	@Override
	public String getMechanism() {
		return "SCRAM-SHA-256";
	}
}
