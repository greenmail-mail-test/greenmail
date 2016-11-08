package com.icegreen.greenmail.filestore;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.mail.Flags;

import com.icegreen.greenmail.imap.ImapConstants;
import sun.management.VMManagement;

/**
 * Created by saladin on 11/2/16.
 */
public class FileStoreUtil {
	//public static final String FILESTORE_ROOT_DIR = "/tmp/filestore";

	private static final int ANSWERED  = 1;  // 00000001
	private static final int DELETED   = 2;  // 00000010
	private static final int DRAFT     = 4;  // 00000100
	private static final int FLAGGED   = 8;  // 00001000
	private static final int RECENT    = 16; // 00010000
	private static final int SEEN      = 32; // 00100000

	public static int getProcessId() {
		try {
			RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
			Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
			jvmField.setAccessible(true);
			VMManagement vmManagement = (VMManagement) jvmField.get(runtimeMXBean);
			Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
			getProcessIdMethod.setAccessible(true);
			Integer processId = (Integer) getProcessIdMethod.invoke(vmManagement);
			if (processId != null) {
				return processId.intValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}


	public static Path convertFullNameToPath(String rootDir, String fullName) {
		String[] mboxTokens = fullName.split(ImapConstants.HIERARCHY_DELIMITER_REGEX);
		return Paths.get(rootDir, mboxTokens);
	}

	public static String convertPathToFullName(String rootDir, Path p) {
		int totCount = p.normalize().getNameCount();
		int rootCount = Paths.get(rootDir).normalize().getNameCount();
		Path subPath = p.subpath(rootCount, totCount);
		return subPath.toString().replace(File.separatorChar, ImapConstants.HIERARCHY_DELIMITER_CHAR);
	}

	public static int convertFlagsToFlagBitSet(Flags flags) {
		int result = 0;
		for (Flags.Flag f : flags.getSystemFlags()) {
			if (Flags.Flag.ANSWERED.equals(f)) {
				result |= ANSWERED;
			} else if (Flags.Flag.DELETED.equals(f)) {
				result |= DELETED;
			} else if (Flags.Flag.DRAFT.equals(f)) {
				result |= DRAFT;
			} else if (Flags.Flag.FLAGGED.equals(f)) {
				result |= FLAGGED;
			} else if (Flags.Flag.RECENT.equals(f)) {
				result |= RECENT;
			} else if (Flags.Flag.SEEN.equals(f)) {
				result |= SEEN;
			}
		}
		return result;
	}

	public static Flags convertFlagBitSetToFlags(int bitset) {
		Flags result = new Flags();
		if ((bitset & ANSWERED) != 0) {
			result.add(Flags.Flag.ANSWERED);
		}
		if ((bitset & DELETED) != 0) {
			result.add(Flags.Flag.DELETED);
		}
		if ((bitset & DRAFT) != 0) {
			result.add(Flags.Flag.DRAFT);
		}
		if ((bitset & FLAGGED) != 0) {
			result.add(Flags.Flag.FLAGGED);
		}
		if ((bitset & RECENT) != 0) {
			result.add(Flags.Flag.RECENT);
		}
		if ((bitset & SEEN) != 0) {
			result.add(Flags.Flag.SEEN);
		}
		return result;
	}

	public static boolean isDeletedFlagSet(int bitset) {
		if ((bitset & DELETED) != 0) {
			return true;
		}
		return false;
	}

	public static boolean isSeenFlagSet(int bitset) {
		if ((bitset & SEEN) != 0) {
			return true;
		}
		return false;
	}

	public static boolean isRecentFlagSet(int bitset) {
		if ((bitset & RECENT) != 0) {
			return true;
		}
		return false;
	}

}
