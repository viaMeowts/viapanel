package com.viameowts.viapanel.api;

import net.minecraft.network.chat.Component;

import java.util.List;

public record ViaPanelSection(String id, Component title, List<String> fields) {
}
