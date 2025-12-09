package com.momosoftworks.irradiated.client.ui.config;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@EventBusSubscriber(Dist.CLIENT)
public abstract class AbstractConfigPage extends Screen {
    // Count how many ticks the mouse has been still for tooltip delay
    static int MOUSE_STILL_TIMER = 0;
    static int TOOLTIP_DELAY = 5;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        MOUSE_STILL_TIMER++;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        MOUSE_STILL_TIMER = 0;
        super.mouseMoved(mouseX, mouseY);
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Post event) {
        if (Minecraft.getInstance().screen instanceof AbstractConfigPage screen) {
            screen.children().forEach(child -> {
                if (child instanceof AbstractWidget widget && !widget.isMouseOver(event.getMouseX(), event.getMouseY())) {
                    widget.setFocused(false);
                }
            });
        }
    }

    protected final Screen parentScreen;
    
    public Map<String, Pair<List<GuiEventListener>, Boolean>> widgetBatches = new HashMap<>();
    public Map<String, List<Component>> tooltips = new HashMap<>();
    
    protected int rightSideLength = 0;
    protected int leftSideLength = 0;
    
    private static final int TITLE_HEIGHT = 16;
    private static final int BOTTOM_BUTTON_HEIGHT_OFFSET = 26;
    private static final int BOTTOM_BUTTON_WIDTH = 150;
    private static final int OPTION_SIZE = 25;
    
    public static Minecraft MINECRAFT = Minecraft.getInstance();
    public static DecimalFormat TWO_PLACES = new DecimalFormat("#.##");
    
    // Texture resources
    private static final ResourceLocation DIVIDER_TEXTURE = ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/sprites/config/divider.png");
    private static final ResourceLocation CLIENTSIDE_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/sprites/config/clientside_icon.png");
    
    // Button sprites
    public static final WidgetSprites CONFIG_BUTTON_SPRITES = new WidgetSprites(
        ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/config/config_button"),
        ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/config/config_button_disabled"),
        ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/config/config_button_focus"));
    
    public static final WidgetSprites NEXT_PAGE_SPRITES = new WidgetSprites(
        ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/sprites/config/next_page"),
        ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/sprites/config/next_page_focus"));
    
    public static final WidgetSprites PREV_PAGE_SPRITES = new WidgetSprites(
        ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/sprites/config/prev_page"),
        ResourceLocation.fromNamespaceAndPath("irradiated", "textures/gui/sprites/config/prev_page_focus"));
    
    ImageButton nextNavButton;
    ImageButton prevNavButton;
    
    public AbstractConfigPage(Screen parentScreen) {
        super(Component.translatable("irradiated.config.title"));
        this.parentScreen = parentScreen;
    }
    
    public AbstractConfigPage(Screen parentScreen, Component title) {
        super(title);
        this.parentScreen = parentScreen;
    }
    
    public abstract int getPageIndex();
    public abstract Component sectionOneTitle();
    @Nullable
    public abstract Component sectionTwoTitle();
    
    public boolean showNavigation() {
        return true;
    }
    
    /**
     * Adds an empty block to the list on the given side. One unit is the height of a button.
     */
    protected void addEmptySpace(Side side, double height) {
        if (side == Side.LEFT) {
            this.leftSideLength += (int) (OPTION_SIZE * height);
        } else {
            this.rightSideLength += (int) (OPTION_SIZE * height);
        }
    }
    
    /**
     * Adds a label with plain text to the list on the given side.
     */
    protected void addLabel(String id, Side side, Component text) {
        int labelX = side == Side.LEFT ? this.width / 2 - 185 : this.width / 2 + 51;
        int labelY = this.height / 4 - 8 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        ConfigLabel label = new ConfigLabel(id, text, labelX, labelY);
        
        this.addWidgetBatch(id, List.of(label), true);
        
        if (side == Side.LEFT) {
            this.leftSideLength += font.lineHeight + 4;
        } else {
            this.rightSideLength += font.lineHeight + 4;
        }
    }
    
    /**
     * Adds a button to the list on the given side.
     */
    protected void addButton(String id, Side side, Supplier<Component> dynamicLabel, Consumer<Button> onClick,
                           boolean requireOP, boolean setsCustomDifficulty, boolean clientside, Component... tooltip) {
        Component label = dynamicLabel.get();
        
        boolean shouldBeActive = !requireOP || MINECRAFT.player == null || MINECRAFT.player.hasPermissions(2);
        int widgetX = this.width / 2 + (side == Side.LEFT ? -179 : 56);
        int widgetY = this.height / 4 - 8 + (side == Side.LEFT ? leftSideLength : rightSideLength);
        // Extend the button if the text is too long
        int buttonWidth = 152 + Math.max(0, font.width(label) - 140);
        
        // Make the button
        Button button = Button.builder(label, button1 -> {
            onClick.accept(button1);
            button1.setMessage(dynamicLabel.get());
        }).pos(widgetX, widgetY).size(buttonWidth, 20).build();
        
        button.active = shouldBeActive;
        
        // Add the clientside indicator
        if (clientside) {
            this.createClientsideIcon(id, widgetX - 16, widgetY + 4);
        }
        
        List<Component> tooltipList = new ArrayList<>(Arrays.asList(tooltip));
        // Add the client disclaimer if the setting is marked clientside
        if (clientside) {
            tooltipList.add(Component.translatable("irradiated.config.clientside_warning").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Assign the tooltip
        this.setTooltip(id, tooltipList);
        
        this.addWidgetBatch(id, List.of(button), shouldBeActive);
        
        // Mark this space as used
        if (side == Side.LEFT)
            this.leftSideLength += OPTION_SIZE;
        else
            this.rightSideLength += OPTION_SIZE;
    }
    
    /**
     * Adds an input that accepts decimal numbers to the list on the given side.
     */
    protected void addDecimalInput(String id, Side side, MutableComponent label, Consumer<Double> onEdited, Consumer<EditBox> onInit,
                                 boolean requireOP, boolean setsCustomDifficulty, boolean clientside, Component... tooltip) {
        boolean shouldBeActive = !requireOP || MINECRAFT.player == null || MINECRAFT.player.hasPermissions(2);
        int labelOffset = font.width(label.getString()) > 90 ? font.width(label.getString()) - 86 : 0;
        int boxWidth = Math.max(51 - labelOffset, 30);
        int widgetX = this.width / 2 + (side == Side.LEFT ? -80 : 155);
        int widgetY = this.height / 4 + (side == Side.LEFT ? this.leftSideLength : this.rightSideLength) - 2;
        
        // Make the input
        EditBox textBox = new EditBox(this.font, widgetX + labelOffset, widgetY - 6, boxWidth, 18, Component.literal("")) {
            public void onEdit() {
                try {
                    onEdited.accept(Double.parseDouble(this.getValue()));
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
            }
            
            @Override
            public void insertText(String text) {
                super.insertText(text);
                this.onEdit();
            }
            
            @Override
            public void deleteWords(int i) {
                super.deleteWords(i);
                this.onEdit();
            }
            
            @Override
            public void deleteChars(int i) {
                super.deleteChars(i);
                this.onEdit();
            }
        };
        
        // Disable the input if the player is not OP
        textBox.setEditable(shouldBeActive);
        
        // Set the initial value
        onInit.accept(textBox);
        
        // Round the input to 2 decimal places
        try {
            textBox.setValue(TWO_PLACES.format(Double.parseDouble(textBox.getValue())));
        } catch (NumberFormatException e) {
            textBox.setValue("0");
        }
        
        // Make the label
        ConfigLabel configLabel = new ConfigLabel(id, label.withStyle(Style.EMPTY.withColor(shouldBeActive ? 16777215 : 8421504)), widgetX - 95, widgetY);
        
        // Add the clientside indicator
        if (clientside) {
            this.createClientsideIcon(id, widgetX - 115, widgetY - 2);
        }
        
        List<Component> tooltipList = new ArrayList<>(Arrays.asList(tooltip));
        // Add the client disclaimer if the setting is marked clientside
        if (clientside) {
            tooltipList.add(Component.translatable("irradiated.config.clientside_warning").withStyle(ChatFormatting.DARK_GRAY));
        }
        // Assign the tooltip
        this.setTooltip(id, tooltipList);
        
        // Add the widget
        this.addWidgetBatch(id, List.of(textBox, configLabel), shouldBeActive);
        
        // Mark this space as used
        if (side == Side.LEFT)
            this.leftSideLength += OPTION_SIZE;
        else
            this.rightSideLength += OPTION_SIZE;
    }
    
    protected void createClientsideIcon(String id, int x, int y) {
        ConfigImageWidget icon = new ConfigImageWidget(CLIENTSIDE_ICON_TEXTURE, x, y, 12, 12, 0, 0);
        this.addRenderableOnly(icon);
        String iconId = String.format("%s_client", id);
        this.setTooltip(iconId, List.of(Component.translatable("irradiated.config.clientside_warning")));
        this.addWidgetBatch(iconId, List.of(icon), true);
    }
    
    @Override
    protected void init() {
        MOUSE_STILL_TIMER = 0;
        this.setDragging(false);
        this.leftSideLength = 0;
        this.rightSideLength = 0;
        
        this.addRenderableWidget(new Button.Builder(
                Component.translatable("gui.done"),
                button -> this.onClose())
            .pos(this.width / 2 - BOTTOM_BUTTON_WIDTH / 2, this.height - BOTTOM_BUTTON_HEIGHT_OFFSET)
            .size(BOTTOM_BUTTON_WIDTH, 20)
            .createNarration(button -> MutableComponent.create(button.get().getContents()))
            .build()
        );
        
        // Navigation
        if (this.showNavigation()) {
            nextNavButton = new ImageButton(this.width - 32, 12, 20, 20, NEXT_PAGE_SPRITES,
                button -> {
                    MINECRAFT.setScreen(IrradiatedConfigScreen.getPage(this.getPageIndex() + 1, parentScreen));
                });
            if (this.getPageIndex() < IrradiatedConfigScreen.getLastPage())
                this.addRenderableWidget(nextNavButton);
            
            prevNavButton = new ImageButton(this.width - 76, 12, 20, 20, PREV_PAGE_SPRITES,
                    button -> {
                        MINECRAFT.setScreen(IrradiatedConfigScreen.getPage(this.getPageIndex() - 1, parentScreen));
                    });
            if (this.getPageIndex() > 0)
                this.addRenderableWidget(prevNavButton);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        Font font = this.font;
        
        // Page Title
        graphics.drawCenteredString(this.font, this.title.getString(), this.width / 2, TITLE_HEIGHT, 0xFFFFFF);
        
        // Page Number
        if (showNavigation()) {
            graphics.drawString(this.font, Component.literal((this.getPageIndex() + 1) + "/" + (IrradiatedConfigScreen.getLastPage() + 1)),
                              this.width - 53, 18, 16777215, true);
        }
        
        // Section 1 Title
        graphics.drawString(this.font, this.sectionOneTitle(), this.width / 2 - 204, this.height / 4 - 28, 16777215, true);
        
        // Section 1 Divider
        graphics.fill(this.width / 2 - 202, this.height / 4 - 16, this.width / 2 - 201, this.height / 4 + 138, 0xFFFFFFFF);
        
        if (this.sectionTwoTitle() != null) {
            // Section 2 Title
            graphics.drawString(this.font, this.sectionTwoTitle(), this.width / 2 + 32, this.height / 4 - 28, 16777215, true);
            
            // Section 2 Divider
            graphics.fill(this.width / 2 + 34, this.height / 4 - 16, this.width / 2 + 35, this.height / 4 + 138, 0xFFFFFFFF);
        }
        
        // Render tooltip
        if (this.isDragging()) {
            MOUSE_STILL_TIMER = 0;
        }
        if (MOUSE_STILL_TIMER < TOOLTIP_DELAY) return;
        
        for (Map.Entry<String, Pair<List<GuiEventListener>, Boolean>> entry : widgetBatches.entrySet()) {
            String id = entry.getKey();
            List<GuiEventListener> widgets = entry.getValue().getFirst();
            boolean enabled = entry.getValue().getSecond();
            int minX = 0, minY = 0, maxX = 0, maxY = 0;
            
            for (GuiEventListener listener : widgets) {
                if (listener instanceof AbstractWidget widget) {
                    if (minX == 0 || widget.getX() < minX)
                        minX = widget.getX();
                    if (minY == 0 || widget.getY() < minY)
                        minY = widget.getY();
                    if (maxX == 0 || widget.getX() + widget.getWidth() > maxX)
                        maxX = widget.getX() + widget.getWidth();
                    if (maxY == 0 || widget.getY() + widget.getHeight() > maxY)
                        maxY = widget.getY() + widget.getHeight();
                }
            }
            
            // if the mouse is hovering over any of the widgets in the batch, show the corresponding tooltip
            if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
                List<Component> tooltipList = enabled
                                            ? this.tooltips.get(id)
                                            : List.of(Component.translatable("irradiated.config.require_op").withStyle(ChatFormatting.RED));
                if (tooltipList != null && !tooltipList.isEmpty()) {
                    graphics.renderTooltip(font, tooltipList, Optional.empty(), mouseX, mouseY);
                }
                break;
            }
        }
    }
    
    public enum Side {
        LEFT, RIGHT
    }
    
    protected void addWidgetBatch(String id, List<GuiEventListener> elements, boolean enabled) {
        for (GuiEventListener element : elements) {
            if (element instanceof Renderable widget)
                this.addRenderableWidget((GuiEventListener & Renderable & NarratableEntry) widget);
        }
        this.widgetBatches.put(id, Pair.of(elements, enabled));
    }
    
    public List<GuiEventListener> getWidgetBatch(String id) {
        return this.widgetBatches.get(id).getFirst();
    }
    
    protected void setTooltip(String id, List<Component> tooltip) {
        List<Component> wrappedTooltip = new ArrayList<>();
        for (Component component : tooltip) {
            // wrap lines at 300 px
            List<FormattedText> wrappedText = font.getSplitter().splitLines(component, 300, component.getStyle());
            // convert FormattedText back to styled Components
            wrappedTooltip.addAll(wrappedText.stream().map(text -> Component.literal(text.getString()).withStyle(component.getStyle())).toList());
        }
        this.tooltips.put(id, wrappedTooltip);
    }
    
    @Override
    public void onClose() {
        MINECRAFT.setScreen(this.parentScreen);
        IrradiatedConfigScreen.saveConfig();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    public MutableComponent getToggleButtonText(MutableComponent text, boolean on) {
        return text.append(": ")
                   .append(on ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }
    
    public <T extends Enum<T> & StringRepresentable> MutableComponent getEnumButtonText(MutableComponent text, T value) {
        return text.append(": ")
                   .append(Component.translatable(value.getSerializedName()));
    }
    
    public <T extends Enum<T>> T getNextCycle(T current) {
        T[] values = current.getDeclaringClass().getEnumConstants();
        int index = (current.ordinal() + 1) % values.length;
        return values[index];
    }
}
