package com.icegreen.greenmail.filestore;

import java.nio.file.Path;
import javax.mail.Flags;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by saladin on 11/2/16.
 */
public class FileStoreUtilTest {


	@Test
	public void testConvertPathToFullName() {
		String name = "a.b.c";
		Path p = FileStoreUtil.convertFullNameToPath("/tmp/saladin", name);
		System.out.println("Path: " + p.toAbsolutePath().toString());

		String na = FileStoreUtil.convertPathToFullName("/tmp/saladin",p);
		System.out.println("Name: " + na);

	}

	@Test
	public void testFlag1() {
		Flags origFlags = new Flags(Flags.Flag.SEEN);
		origFlags.add(Flags.Flag.ANSWERED);
		int bitset = FileStoreUtil.convertFlagsToFlagBitSet(origFlags);
		Flags newFlags = FileStoreUtil.convertFlagBitSetToFlags(bitset);
		Assert.assertEquals(origFlags, newFlags);
	}

	@Test
	public void testFlag2() {
		Flags origFlags = new Flags(Flags.Flag.SEEN);
		origFlags.add(Flags.Flag.ANSWERED);
		origFlags.add(Flags.Flag.RECENT);
		origFlags.add(Flags.Flag.DRAFT);
		origFlags.add(Flags.Flag.DELETED);

		int bitset = FileStoreUtil.convertFlagsToFlagBitSet(origFlags);
		Flags newFlags = FileStoreUtil.convertFlagBitSetToFlags(bitset);
		Assert.assertEquals(origFlags, newFlags);
	}

	private static void printFlags(Flags flags) {
	}


}
