package me.grax.jbytemod.utils;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.ClassReader.*;

/**
 * Utilities for dealing with class-file loading/parsing.
 *
 * @author Matt
 */
public class ClassUtil {
	/**
	 * The offset from which a version and the version constant value is. For example, Java 8 is 52 <i>(44 + 8)</i>.
	 */
	public static final int VERSION_OFFSET = 44;

	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return {@link org.objectweb.asm.ClassReader} loaded from runtime.
	 */
	public static ClassReader fromRuntime(String name) {
		try {
			return new ClassReader(name);
		} catch(IOException e) {
			// Expected / allowed: ignore these
		} catch(Exception ex) {
			// Unexpected
			throw new IllegalStateException("Failed to load class from runtime: " + name, ex);
		}
		return null;
	}

	/**
	 * @param reader
	 * 		Class reader to generate a node from.
	 * @param readFlags
	 * 		Flags to apply when generating the node.
	 *
	 * @return Node from reader.
	 */
	public static ClassNode getNode(ClassReader reader, int readFlags) {
		ClassNode node = new ClassNode();
		reader.accept(node, readFlags);
		return node;
	}

	/**
	 * @param node
	 * 		Node to convert back to bytecode.
	 * @param writeFlags
	 * 		Writer flags to use in conversion.
	 *
	 * @return Class bytecode.
	 */
	public static byte[] toCode(ClassNode node, int writeFlags) {
		ClassWriter cw = new ClassWriter(writeFlags);
		node.accept(cw);
		return cw.toByteArray();
	}

	/**
	 * @param code
	 * 		Class bytecode.
	 *
	 * @return Class access. If an parse error occurred then return is {@code 0}.
	 */
	public static int getAccess(byte[] code) {
		try {
			return new ClassReader(code).getAccess();
		} catch(Exception ex) { /* Bad class file? */ return 0;}
	}

	/**
	 * @param code
	 * 		Class bytecode.
	 *
	 * @return Class major version. If an parse error occurred then return is {@link Opcodes#V1_8}.
	 */
	public static int getVersion(byte[] code) {
		try {
			return (((code[6] & 0xFF) << 8) | (code[7] & 0xFF));
		} catch(Exception ex) { /* Bad class file? */ return Opcodes.V1_8;}
	}

	/**
	 * @param data
	 * 		Potential class bytecode.
	 *
	 * @return {@code true} if data has class magic prefix.
	 */
	public static boolean isClass(byte[] data) {
		return data.length > 4 &&
				0xCAFEBABEL == ((
						(0xFF & data[0]) << 24L |
						(0xFF & data[1]) << 16L |
						(0xFF & data[2]) << 8L  |
						 0xFF & data[3]) & 0xFFFFFFFFL);
	}

	@SuppressWarnings("all")
	private static void updateAnnotationList(List to, List from) {
		// No data to copy
		if (from == null)
			return;
		// Add if not null
		if (to != null)
			to.addAll(from);
	}

	/**
	 * Copies field metadata.
	 *
	 * @param from field to copy from.
	 * @param to field to copy to.
	 */
	public static void copyFieldMetadata(FieldNode from, FieldNode to) {
		to.invisibleAnnotations = from.invisibleAnnotations;
		to.visibleAnnotations = from.visibleAnnotations;
		to.invisibleTypeAnnotations = from.invisibleTypeAnnotations;
		to.visibleTypeAnnotations = from.visibleTypeAnnotations;
	}

	/**
	 * Strip debug information from the given class bytecode.
	 *
	 * @param code
	 * 		Class bytecode.
	 *
	 * @return Class bytecode, modified to remove all debug information.
	 */
	public static byte[] stripDebugForDecompile(byte[] code) {
		if (code == null || code.length <= 10)
			return code;
		ClassReader cr = new ClassReader(code);
		ClassWriter cw = new ClassWriter(0);
		cr.accept(cw, SKIP_DEBUG | EXPAND_FRAMES);
		return cw.toByteArray();
	}

	/**
	 * Validate the class can be parsed by ASM.
	 *
	 * @param value
	 * 		Class bytecode.
	 *
	 * @return {@code true} when the class can be read by ASM.
	 */
	public static boolean isValidClass(byte[] value) {
		if (!isClass(value))
			return false;
		try {
			getNode(new ClassReader(value), SKIP_FRAMES);
			return true;
		} catch(Throwable t) {
			return false;
		}
	}

}
