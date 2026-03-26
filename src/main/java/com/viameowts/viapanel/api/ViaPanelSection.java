package com.viameowts.viapanel.api;

import net.minecraft.text.Text;

import java.util.List;

public record ViaPanelSection(String id, Text title, List<String> fields) {
}
