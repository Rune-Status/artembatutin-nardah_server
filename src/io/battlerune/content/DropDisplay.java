package io.battlerune.content;

import io.battlerune.game.world.entity.actor.npc.definition.NpcDefinition;
import io.battlerune.game.world.entity.actor.npc.drop.NpcDrop;
import io.battlerune.game.world.entity.actor.npc.drop.NpcDropManager;
import io.battlerune.game.world.entity.actor.npc.drop.NpcDropTable;
import io.battlerune.game.world.entity.actor.player.Player;
import io.battlerune.game.world.items.Item;
import io.battlerune.game.world.items.ItemDefinition;
import io.battlerune.net.packet.out.SendItemOnInterfaceSlot;
import io.battlerune.net.packet.out.SendScrollbar;
import io.battlerune.net.packet.out.SendString;
import io.battlerune.net.packet.out.SendTooltip;
import io.battlerune.util.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles displaying the npc drops on an itemcontainer.
 * @author Daniel
 */
public class DropDisplay {
	
	private static final String[] DEFAULT = {"Man", "Goblin", "Dragon"};
	
	public static void open(Player player) {
		search(player, Utility.randomElement(DEFAULT), DropType.NPC);
		List<Integer> key = player.attributes.get("DROP_DISPLAY_KEY", List.class);
		display(player, NpcDropManager.NPC_DROPS.get(key.get(0)));
		player.interfaceManager.open(54500);
	}
	
	public static void search(Player player, String context, DropType type) {
		context = context.trim().toLowerCase();
		List<String> npc = new ArrayList<>();
		List<Integer> integer = new ArrayList<>();
		for(Map.Entry<Integer, NpcDropTable> drop : NpcDropManager.NPC_DROPS.entrySet()) {
			NpcDropTable definition = drop.getValue();
			if(NpcDefinition.get(definition.npcIds[0]) == null)
				continue;
			String name = NpcDefinition.get(definition.npcIds[0]).getName();
			if(type == DropType.NPC) {
				if(name.toLowerCase().contains(context)) {
					if(!npc.contains(name)) {
						npc.add(name);
						integer.add(definition.npcIds[0]);
					}
				}
			} else if(type == DropType.ITEM) {
				for(NpcDrop item : definition.drops) {
					String itemName = ItemDefinition.get(item.item).getName();
					if(itemName.toLowerCase().contains(context)) {
						if(!npc.contains(name)) {
							npc.add(name);
							integer.add(definition.npcIds[0]);
						}
					}
				}
			}
		}
		
		if(integer.isEmpty()) {
			player.dialogueFactory.sendStatement("No search was found for your entry!").execute();
			return;
		}
		
		int size = npc.size() < 10 ? 10 : npc.size();
		for(int index = 0, string = 54516; index < size; index++, string += 2) {
			if(string >= 54551)
				break;
			String name = index >= npc.size() ? "" : npc.get(index);
			player.send(new SendTooltip(name.isEmpty() ? "" : "View drop table of <col=ff9933>" + name, string));
			player.send(new SendString(name, string + 1));
		}
		player.attributes.set("DROP_DISPLAY_KEY", integer);
		player.send(new SendScrollbar(54515, 280));
		display(player, NpcDropManager.NPC_DROPS.get(0));
	}
	
	public static void display(Player player, NpcDropTable definition) {
		int size = definition == null ? 9 : definition.drops.length < 9 ? 9 : definition.drops.length;
		player.send(new SendScrollbar(54550, definition == null ? 350 : (size * 32)));
		player.send(new SendString(definition == null ? "" : "Drops: " + definition.drops.length, 54514));
		player.send(new SendString(definition == null ? "" : "" + NpcDefinition.get(definition.npcIds[0]).getName() + "(" + NpcDefinition.get(definition.npcIds[0]).getId() + ")", 54513));
		for(int index = 0, string = 54552; index < size; index++) {
			boolean valid = definition != null && (index < definition.drops.length && (NpcDefinition.get(definition.npcIds[0]) != null));
			NpcDrop drop = valid ? definition.drops[index] : null;
			Item item = valid ? new Item(drop.item, drop.maximum) : null;
			player.send(new SendItemOnInterfaceSlot(54551, item, index));
			string++;
			player.send(new SendString(!valid ? "" : item.getName() + " (<col=ffffff>" + item.getId() + "</col>)", string));
			string++;
			player.send(new SendString(!valid ? "" : Utility.formatDigits(drop.minimum), string));
			string++;
			player.send(new SendString(!valid ? "" : Utility.formatDigits(drop.maximum), string));
			string++;
			player.send(new SendString(!valid ? "" : drop.type.toString(), string));
			string++;
		}
	}
	
	public enum DropType {
		ITEM, NPC
	}
}