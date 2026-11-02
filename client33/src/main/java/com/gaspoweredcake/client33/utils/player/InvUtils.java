
package com.gaspoweredcake.client33.utils.player;

import com.gaspoweredcake.client33.mixininterface.IMultiPlayerGameMode;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Range;

import java.util.function.Predicate;

import static com.gaspoweredcake.client33.Client33.mc;

/**
 * Util class for dealing inventories and the items in them.
 * See {@link SlotUtils} for details on how slots are referenced.
 */
public class InvUtils {
    public static int previousSlot = -1;
    private static Object swapOwner;

    private InvUtils() {
    }

    // Predicates

    private static Predicate<ItemStack> isOneOf(Item... items) {
        return itemStack -> {
            for (var item : items) if (itemStack.is(item)) return true;
            return false;
        };
    }

    public static boolean testInMainHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getMainHandItem());
    }

    public static boolean testInMainHand(Item... items) {
        return testInMainHand(isOneOf(items));
    }

    public static boolean testInOffHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getOffhandItem());
    }

    public static boolean testInOffHand(Item... items) {
        return testInOffHand(isOneOf(items));
    }

    public static boolean testInHands(Predicate<ItemStack> predicate) {
        return testInMainHand(predicate) || testInOffHand(predicate);
    }

    public static boolean testInHands(Item... items) {
        return testInMainHand(items) || testInOffHand(items);
    }

    public static boolean testInHotbar(Predicate<ItemStack> predicate) {
        if (testInHands(predicate)) return true;

        for (int i = SlotUtils.HOTBAR_START; i <= SlotUtils.HOTBAR_END; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (predicate.test(stack)) return true;
        }

        return false;
    }

    public static boolean testInHotbar(Item... items) {
        return testInHotbar(isOneOf(items));
    }

    // Finding items

    public static FindItemResult findEmpty() {
        return find(ItemStack::isEmpty);
    }

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(isOneOf(items));
    }

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (mc.player == null) return new FindItemResult(-1, 0);
        if (testInOffHand(isGood)) {
            return new FindItemResult(SlotUtils.OFFHAND, mc.player.getOffhandItem().getCount());
        }

        if (testInMainHand(isGood)) {
            return new FindItemResult(mc.player.getInventory().getSelectedSlot(), mc.player.getMainHandItem().getCount());
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Item... items) {
        return find(isOneOf(items));
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        if (mc.player == null) return new FindItemResult(-1, 0);
        return find(isGood, 0, mc.player.getInventory().getContainerSize() - 1);
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        if (mc.player == null) return new FindItemResult(-1, 0);

        int slot = -1, count = 0;

        int size = mc.player.getInventory().getContainerSize();
        for (int i = Math.max(0, start); i <= Math.min(end, size - 1); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);

            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
            }
        }

        return new FindItemResult(slot, count);
    }

    public static FindItemResult findFastestTool(BlockState state) {
        float bestScore = 1;
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isCorrectToolForDrops(state)) continue;

            float score = stack.getDestroySpeed(state);
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }

        return new FindItemResult(slot, 1);
    }

    // Interactions

    public static boolean swap(int slot, boolean swapBack) {
        if (mc.player == null || mc.gameMode == null) return false;
        if (swapOwner != mc.player) { previousSlot = -1; swapOwner = mc.player; }
        if (slot == SlotUtils.OFFHAND) return true;
        if (slot < 0 || slot > 8) return false;
        if (swapBack && previousSlot == -1) previousSlot = mc.player.getInventory().getSelectedSlot();
        else if (!swapBack) previousSlot = -1;

        mc.player.getInventory().setSelectedSlot(slot);
        ((IMultiPlayerGameMode) mc.gameMode).client33$syncSelected();
        return true;
    }

    public static boolean swapBack() {
        if (previousSlot == -1 || swapOwner != mc.player) { previousSlot = -1; return false; }

        boolean return_ = swap(previousSlot, false);
        previousSlot = -1;
        return return_;
    }

    public static Action move() {
        Action action = new Action();
        action.type = ContainerInput.PICKUP;
        action.two = true;
        return action;
    }

    public static Action click() {
        Action action = new Action();
        action.type = ContainerInput.PICKUP;
        return action;
    }

    /**
     * When writing code with quickSwap, both to and from should provide the ID of a slot, not the index.
     * From should be the slot in the hotbar, to should be the slot you're switching an item from.
     */
    public static Action quickSwap() {
        Action action = new Action();
        action.type = ContainerInput.SWAP;
        return action;
    }

    public static Action shiftClick() {
        Action action = new Action();
        action.type = ContainerInput.QUICK_MOVE;
        return action;
    }

    public static Action drop() {
        Action action = new Action();
        action.type = ContainerInput.THROW;
        action.data = 1;
        return action;
    }

    public static Action dropOne() {
        Action action = new Action();
        action.type = ContainerInput.THROW;
        action.data = 0;
        return action;
    }

    public static void dropHand() {
        if (!mc.player.containerMenu.getCarried().isEmpty())
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, 0, ContainerInput.PICKUP, mc.player);
    }

    public static class Action {
        private ContainerInput type = null;
        private boolean two = false;
        private int from = -1;
        private int to = -1;
        private int data = 0;

        private final AbstractContainerMenu menu = mc.player == null ? null : mc.player.containerMenu;

        private Action() {
        }

        // From

        public Action fromId(int id) {
            from = id;
            return this;
        }

        /**
         * @param index The index of one of the slots within the inventory
         */
        public Action from(int index) {
            return fromId(SlotUtils.indexToId(index));
        }

        public Action fromHotbar(@Range(from = 0, to = 8) int i) {
            return from(SlotUtils.HOTBAR_START + i);
        }

        public Action fromOffhand() {
            return from(SlotUtils.OFFHAND);
        }

        public Action fromMain(@Range(from = 0, to = 26) int i) {
            return from(SlotUtils.MAIN_START + i);
        }

        /**
         * @param i The entity slot id of one of the four humanoid armor pieces, as defined in {@link EquipmentSlot}
         */
        public Action fromArmor(int i) {
            return from(SlotUtils.armorIndex(i));
        }

        // To

        public void toId(int id) {
            to = id;
            run();
        }

        /**
         * @param index The index of one of the slots within the inventory
         */
        public void to(int index) {
            toId(SlotUtils.indexToId(index));
        }

        public void toHotbar(@Range(from = 0, to = 8) int i) {
            to(SlotUtils.HOTBAR_START + i);
        }

        public void toOffhand() {
            to(SlotUtils.OFFHAND);
        }

        public void toMain(@Range(from = 0, to = 26) int i) {
            to(SlotUtils.MAIN_START + i);
        }

        /**
         * @param i The entity slot id of one of the four humanoid armor pieces, as defined in {@link EquipmentSlot}
         */
        public void toArmor(int i) {
            to(SlotUtils.armorIndex(i));
        }

        // Slot

        public void slotId(int id) {
            from = to = id;
            run();
        }

        /**
         * @param index The index of one of the slots within the inventory
         */
        public void slot(int index) {
            slotId(SlotUtils.indexToId(index));
        }

        public void slotHotbar(@Range(from = 0, to = 8) int i) {
            slot(SlotUtils.HOTBAR_START + i);
        }

        public void slotOffhand() {
            slot(SlotUtils.OFFHAND);
        }

        public void slotMain(@Range(from = 0, to = 26) int i) {
            slot(SlotUtils.MAIN_START + i);
        }

        /**
         * @param i The entity slot id of one of the four humanoid armor pieces, as defined in {@link EquipmentSlot}
         */
        public void slotArmor(int i) {
            slot(SlotUtils.armorIndex(i));
        }

        // Other

        private void run() {
            if (mc.player == null || mc.gameMode == null || menu == null || menu != mc.player.containerMenu) return;
            boolean hadEmptyCursor = menu.getCarried().isEmpty();

            if (type == ContainerInput.SWAP) {
                data = from;
                from = to;
            }

            if (type != null && from != -1 && to != -1) {
                click(from);
                if (two) click(to);
            }

            ContainerInput preType = type;
            boolean preTwo = two;
            int preFrom = from;
            int preTo = to;

            type = null;
            two = false;
            from = -1;
            to = -1;
            data = 0;

            if (mc.player != null && mc.player.containerMenu == menu && hadEmptyCursor && preType == ContainerInput.PICKUP && preTwo && (preFrom != -1 && preTo != -1) && !mc.player.containerMenu.getCarried().isEmpty()) {
                InvUtils.click().slotId(preFrom);
            }
        }

        private void click(int id) {
            if (mc.player != null && mc.gameMode != null && mc.player.containerMenu == menu)
                mc.gameMode.handleContainerInput(menu.containerId, id, data, type, mc.player);
        }
    }
}
