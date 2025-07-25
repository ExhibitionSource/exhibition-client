package exhibition.management.command.impl;

import exhibition.management.command.Command;
import net.minecraft.client.Minecraft;

public class Clear extends Command {

	public Clear(String[] names, String description) {
		super(names, description);
	}

	@Override
	public void fire(String[] args) {
		Minecraft.getMinecraft().ingameGUI.getChatGUI().clearChatMessages();
	}

	@Override
	public String getUsage() {
		return "Clear";
	}

}
