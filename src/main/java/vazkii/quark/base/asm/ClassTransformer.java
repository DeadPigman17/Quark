/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Quark Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Quark
 *
 * Quark is Open Source and distributed under the
 * CC-BY-NC-SA 3.0 License: https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_GB
 *
 * File Created @ [26/03/2016, 21:31:04 (GMT)]
 */
package vazkii.quark.base.asm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;

public class ClassTransformer implements IClassTransformer {
	
	private static final String ASM_HOOKS = "vazkii/quark/base/asm/ASMHooks";

	public static final ClassnameMap CLASS_MAPPINGS = new ClassnameMap(
		"net/minecraft/entity/Entity", "sm",
		"net/minecraft/item/ItemStack", "afi",
		"net/minecraft/client/renderer/block/model/IBakedModel", "cbe",
		"net/minecraft/entity/EntityLivingBase", "sv",
		"net/minecraft/inventory/EntityEquipmentSlot", "sr",
		"net/minecraft/client/renderer/entity/RenderLivingBase", "bvi",
		"net/minecraft/client/model/ModelBase", "blt",
		"net/minecraft/util/DamageSource", "rx",
		"net/minecraft/entity/item/EntityBoat", "abw",
		"net/minecraft/world/World", "ajq",
		"net/minecraft/util/math/BlockPos", "co",
		"net/minecraft/util/EnumFacing", "cv"
	);
	
	private static final Map<String, Transformer> transformers = new HashMap();

	static {
		// For Emotes
		transformers.put("net.minecraft.client.model.ModelBiped", ClassTransformer::transformModelBiped);
		transformers.put("micdoodle8.mods.galacticraft.core.client.model.ModelPlayerGC", ClassTransformer::transformModelBiped);

		// For Color Runes
		transformers.put("net.minecraft.client.renderer.RenderItem", ClassTransformer::transformRenderItem);
		transformers.put("net.minecraft.client.renderer.entity.layers.LayerArmorBase", ClassTransformer::transformLayerArmorBase);

		// For Boat Sails
		transformers.put("net.minecraft.client.renderer.entity.RenderBoat", ClassTransformer::transformRenderBoat);
		transformers.put("net.minecraft.entity.item.EntityBoat", ClassTransformer::transformEntityBoat);
		
		// For Piston Block Breakers
		transformers.put("net.minecraft.block.BlockPistonBase", ClassTransformer::transformBlockPistonBase);
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if(transformers.containsKey(transformedName))
			return transformers.get(transformedName).apply(basicClass);

		return basicClass;
	}

	private static byte[] transformModelBiped(byte[] basicClass) {
		log("Transforming ModelBiped");
		MethodSignature sig = new MethodSignature("setRotationAngles", "func_78087_a", "a", "(FFFFFFLnet/minecraft/entity/Entity;)V");

		return transform(basicClass, Pair.of(sig, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.RETURN;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 7));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "updateEmotes", "(Lnet/minecraft/entity/Entity;)V"));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));
	}

	private static byte[] transformRenderItem(byte[] basicClass) {
		log("Transforming RenderItem");
		MethodSignature sig1 = new MethodSignature("renderItem", "func_180454_a", "a", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V");
		MethodSignature sig2 = new MethodSignature("renderEffect", "func_180451_a", "a", "(Lnet/minecraft/client/renderer/block/model/IBakedModel;)V");

		byte[] transClass = basicClass;

		transClass = transform(transClass, Pair.of(sig1, combine(
				(AbstractInsnNode node) -> { // Filter
					return true;
				}, (MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "setColorRuneTargetStack", "(Lnet/minecraft/item/ItemStack;)V"));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));

		transClass = transform(transClass, Pair.of(sig2, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.LDC && ((LdcInsnNode) node).cst.equals(-8372020);
				}, (MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "getRuneColor", "()I"));

					method.instructions.insertBefore(node, newInstructions);
					method.instructions.remove(node);
					return false;
				})));

		return transClass;
	}

	static int invokestaticCount = 0;
	private static byte[] transformLayerArmorBase(byte[] basicClass) {
		log("Transforming LayerArmorBase");
		MethodSignature sig1 = new MethodSignature("renderArmorLayer", "func_188361_a", "a", "(Lnet/minecraft/entity/EntityLivingBase;FFFFFFFLnet/minecraft/inventory/EntityEquipmentSlot;)V");
		MethodSignature sig2 = new MethodSignature("renderEnchantedGlint", "func_188364_a", "a", "(Lnet/minecraft/client/renderer/entity/RenderLivingBase;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/model/ModelBase;FFFFFFF)V");

		byte[] transClass = basicClass;

		transClass = transform(transClass, Pair.of(sig1, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.ASTORE;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 10));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "setColorRuneTargetStack", "(Lnet/minecraft/item/ItemStack;)V"));

					method.instructions.insert(node, newInstructions);
					return true;
				})));

		try {
			if(Class.forName("optifine.OptiFineTweaker") != null)
				log("Optifine Detected. Disabling Patch for " + sig2);
		} catch (ClassNotFoundException e) {
			invokestaticCount = 0;
			transClass = transform(transClass, Pair.of(sig2, combine(
					(AbstractInsnNode node) -> { // Filter
						return node.getOpcode() == Opcodes.INVOKESTATIC;
					},
					(MethodNode method, AbstractInsnNode node) -> { // Action
						invokestaticCount++;
						if(invokestaticCount != 5 && invokestaticCount != 8)
							return false;

						InsnList newInstructions = new InsnList();

						newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "applyRuneColor", "(FFFF)V"));

						method.instructions.insertBefore(node, newInstructions);
						method.instructions.remove(node);
						return invokestaticCount == 8;
					})));
		}

		return transClass;
	}

	private static byte[] transformEntityBoat(byte[] basicClass) {
		log("Transforming EntityBoat");
		MethodSignature sig1 = new MethodSignature("attackEntityFrom", "func_76986_a", "a", "(Lnet/minecraft/util/DamageSource;F)Z");
		MethodSignature sig2 = new MethodSignature("onUpdate", "func_70071_h_", "A", "()V");
		
		byte[] transClass = transform(basicClass, Pair.of(sig1, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.POP;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "dropBoatBanner", "(Lnet/minecraft/entity/item/EntityBoat;)V"));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));
		
		transClass = transform(transClass, Pair.of(sig2, combine(
				(AbstractInsnNode node) -> { // Filter
					return true;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "onBoatUpdate", "(Lnet/minecraft/entity/item/EntityBoat;)V"));

					method.instructions.insertBefore(node, newInstructions);
					return true;
				})));
		
		return transClass;
	}

	private static byte[] transformRenderBoat(byte[] basicClass) {
		log("Transforming RenderBoat");
		MethodSignature sig = new MethodSignature("doRender", "func_76986_a", "a", "(Lnet/minecraft/entity/item/EntityBoat;DDDFF)V");

		return transform(basicClass, Pair.of(sig, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.INVOKEVIRTUAL && (((MethodInsnNode) node).desc.equals("(Lnet/minecraft/entity/Entity;FFFFFF)V") || ((MethodInsnNode) node).desc.equals("(Lsm;FFFFFF)V"));
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					log("Patching " + method + " in node " + node);
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
					newInstructions.add(new VarInsnNode(Opcodes.FLOAD, 9));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "renderBannerOnBoat", "(Lnet/minecraft/entity/item/EntityBoat;F)V"));

					method.instructions.insert(node, newInstructions);
					return true;
				})));
	}
	
	private static byte[] transformBlockPistonBase(byte[] basicClass) {
		log("Transforming BlockPistonBase");
		MethodSignature sig = new MethodSignature("doMove", "func_176319_a", "a", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Z)Z");

		return transform(basicClass, Pair.of(sig, combine(
				(AbstractInsnNode node) -> { // Filter
					return node.getOpcode() == Opcodes.ASTORE && ((VarInsnNode) node).var == 11;
				},
				(MethodNode method, AbstractInsnNode node) -> { // Action
					log("Patching " + method + " in node " + node);
					InsnList newInstructions = new InsnList();

					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 6));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 8));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 11));
					newInstructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ASM_HOOKS, "breakStuffWithSpikes", "(Lnet/minecraft/world/World;Ljava/util/List;Ljava/util/List;Lnet/minecraft/util/EnumFacing;Z)Z"));
					
					// recalculate the list and array sizes
					LabelNode label = new LabelNode();
					newInstructions.add(new JumpInsnNode(Opcodes.IFEQ, label));
					
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 6));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I"));
					newInstructions.add(new VarInsnNode(Opcodes.ALOAD, 8));
					newInstructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I"));
					newInstructions.add(new InsnNode(Opcodes.IADD));
					newInstructions.add(new VarInsnNode(Opcodes.ISTORE, 9));
					newInstructions.add(new VarInsnNode(Opcodes.ILOAD, 9));
					
					AbstractInsnNode newNode = node.getPrevious();
					while(true) {
						if(newNode.getOpcode() == Opcodes.ANEWARRAY) {
							newInstructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, ((TypeInsnNode) newNode).desc));
							break;
						}
						newNode = newNode.getPrevious();
					}
					
					newInstructions.add(new VarInsnNode(Opcodes.ASTORE, 10));
					newInstructions.add(label);

					method.instructions.insert(node, newInstructions);
					return true;
				})));
	}

	// BOILERPLATE BELOW ==========================================================================================================================================

	private static byte[] transform(byte[] basicClass, Pair<MethodSignature, MethodAction>... methods) {
		ClassReader reader = new ClassReader(basicClass);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);

		boolean didAnything = false;

		for(Pair<MethodSignature, MethodAction> pair : methods) {
			log("Applying Transformation to method (" + pair.getLeft() + ")");
			didAnything |= findMethodAndTransform(node, pair.getLeft(), pair.getRight());
		}

		if(didAnything) {
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			node.accept(writer);
			return writer.toByteArray();
		}

		return basicClass;
	}

	public static boolean findMethodAndTransform(ClassNode node, MethodSignature sig, MethodAction pred) {
		String funcName = sig.funcName;
		if(LoadingPlugin.runtimeDeobfEnabled)
			funcName = sig.srgName;

		for(MethodNode method : node.methods) {
			if((method.name.equals(funcName)|| method.name.equals(sig.obfName)) && (method.desc.equals(sig.funcDesc) || method.desc.equals(sig.obfDesc))) {
				log("Located Method, patching...");

				boolean finish = pred.test(method);
				log("Patch result: " + finish);

				return finish;
			}
		}

		return false;
	}

	public static MethodAction combine(NodeFilter filter, NodeAction action) {
		return (MethodNode mnode) -> applyOnNode(mnode, filter, action);
	}

	public static boolean applyOnNode(MethodNode method, NodeFilter filter, NodeAction action) {
		Iterator<AbstractInsnNode> iterator = method.instructions.iterator();

		boolean didAny = false;
		while(iterator.hasNext()) {
			AbstractInsnNode anode = iterator.next();
			if(filter.test(anode)) {
				didAny = true;
				if(action.test(method, anode))
					break;
			}
		}

		return didAny;
	}

	private static void log(String str) {
		FMLLog.info("[Quark ASM] %s", str);
	}

	private static class MethodSignature {
		String funcName, srgName, obfName, funcDesc, obfDesc;

		public MethodSignature(String funcName, String srgName, String obfName, String funcDesc) {
			this.funcName = funcName;
			this.srgName = srgName;
			this.obfName = obfName;
			this.funcDesc = funcDesc;
			this.obfDesc = obfuscate(funcDesc);
		}

		@Override
		public String toString() {
			return "Names [" + funcName + ", " + srgName + ", " + obfName + "] Descriptor " + funcDesc + " / " + obfDesc;
		}
		
		private String obfuscate(String desc) {
			for(String s : CLASS_MAPPINGS.keySet())
				if(desc.contains(s))
					desc = desc.replaceAll(s, CLASS_MAPPINGS.get(s));
			
			return desc;
		}

	}

	// Basic interface aliases to not have to clutter up the code with generics over and over again
	private static interface Transformer extends Function<byte[], byte[]> { }
	private static interface MethodAction extends Predicate<MethodNode> { }
	private static interface NodeFilter extends Predicate<AbstractInsnNode> { }
	private static interface NodeAction extends BiPredicate<MethodNode, AbstractInsnNode> { }

}