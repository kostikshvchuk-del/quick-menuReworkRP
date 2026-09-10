package xyz.imcodist.ui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import xyz.imcodist.other.ActionButtonDataHandler;
import xyz.imcodist.ui.surfaces.SwitcherSurface;

import java.util.List;

public class ProfileSelectorUI extends BaseOwoScreen<FlowLayout> {
    public boolean previousEditMode = false;

    public ProfileSelectorUI() {}

    public ProfileSelectorUI(boolean editMode) {
        this.previousEditMode = editMode;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout mainLayout = Containers.verticalFlow(Sizing.fixed(230), Sizing.fixed(220));
        mainLayout.surface(new SwitcherSurface());
        rootComponent.child(mainLayout);

        // Header.
        FlowLayout headerLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(24));
        headerLayout
                .surface(new SwitcherSurface(true))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        mainLayout.child(headerLayout);

        LabelComponent headerLabel = Components.label(Text.translatable("menu.profile.title"));
        headerLabel.shadow(true);
        headerLayout.child(headerLabel);

        // Current profile label.
        FlowLayout currentLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        currentLayout.padding(Insets.of(4, 4, 4, 4));
        mainLayout.child(currentLayout);

        LabelComponent currentLabel = Components.label(
                Text.literal("Current: " + ActionButtonDataHandler.getCurrentProfileDisplayName())
        );
        currentLayout.child(currentLabel);

        // Profile list.
        FlowLayout listLayout = Containers.verticalFlow(Sizing.fill(100), Sizing.content());
        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(
                Sizing.fill(100), Sizing.fixed(110), listLayout);
        scroll.padding(Insets.of(2, 5, 2, 2));
        mainLayout.child(scroll);

        List<String> profiles = ActionButtonDataHandler.listProfiles();
        String current = ActionButtonDataHandler.getCurrentProfile();

        for (String profile : profiles) {
            boolean isActive = profile.equals(current);
            String display = profile.toLowerCase().endsWith(".json")
                    ? profile.substring(0, profile.length() - 5)
                    : profile;

            FlowLayout row = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
            row.padding(Insets.of(2, 0, 0, 2))
                    .alignment(HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);

            LabelComponent nameLabel = Components.label(
                    Text.literal((isActive ? "> " : "") + display)
            );
            nameLabel.positioning(Positioning.relative(0, 50));
            // Let label take remaining space.
            nameLabel.horizontalSizing(Sizing.fill(60));
            row.child(nameLabel);

            if (!isActive) {
                ButtonComponent selectButton = Components.button(
                        Text.translatable("menu.profile.select"),
                        (btn) -> {
                            ActionButtonDataHandler.switchProfile(profile);
                            refresh();
                        });
                selectButton.margins(Insets.of(0, 0, 4, 0));
                row.child(selectButton);

                ButtonComponent deleteButton = Components.button(
                        Text.literal(" X "),
                        (btn) -> {
                            ActionButtonDataHandler.deleteProfile(profile);
                            refresh();
                        });
                deleteButton.margins(Insets.of(0, 0, 0, 0));
                row.child(deleteButton);
            } else {
                LabelComponent activeLabel = Components.label(Text.translatable("menu.profile.active"));
                activeLabel.margins(Insets.of(0, 0, 4, 0));
                row.child(activeLabel);
            }

            listLayout.child(row);
        }

        // New profile row.
        FlowLayout newLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        newLayout.padding(Insets.of(6, 5, 4, 4))
                .alignment(HorizontalAlignment.RIGHT, VerticalAlignment.CENTER);
        mainLayout.child(newLayout);

        TextBoxComponent nameBox = Components.textBox(Sizing.fill(60), "");
        nameBox.setMaxLength(64);
        // Placeholder via tooltip since owo 0.12 TextBox has no placeholder.
        nameBox.tooltip(Text.translatable("menu.profile.new_tooltip"));
        newLayout.child(nameBox);

        ButtonComponent createButton = Components.button(
                Text.translatable("menu.profile.create"),
                (btn) -> {
                    String name = nameBox.getText();
                    if (name == null || name.trim().isEmpty()) return;
                    ActionButtonDataHandler.createProfile(name);
                    refresh();
                });
        createButton.margins(Insets.of(0, 0, 0, 4));
        newLayout.child(createButton);

        // Back button.
        FlowLayout bottomLayout = Containers.horizontalFlow(Sizing.fill(100), Sizing.content());
        bottomLayout.horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(2, 0, 0, 0));
        mainLayout.child(bottomLayout);

        ButtonComponent backButton = Components.button(
                Text.translatable("menu.profile.back"),
                (btn) -> back());
        bottomLayout.child(backButton);
    }

    private void refresh() {
        if (client == null) return;
        ProfileSelectorUI fresh = new ProfileSelectorUI(previousEditMode);
        client.setScreen(fresh);
    }

    private void back() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        MainUI menu = new MainUI();
        menu.editMode = previousEditMode;
        client.setScreen(menu);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
