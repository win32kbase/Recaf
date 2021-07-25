package me.coley.recaf.ui.panel;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import me.coley.recaf.BuildConfig;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**
 * Welcome panel to show when opening Recaf. Includes helpful links / items.
 *
 * @author Matt Coley
 */
public class WelcomePanel extends FlowPane {
	private static final int COMMON_GAP = 20;
	private static final Logger logger = Logging.get(WelcomePanel.class);

	/**
	 * Create the welcome panel.
	 */
	public WelcomePanel() {
		setupSpacing();
		setupChildSizeFormatter();
		addChildren();
	}

	/**
	 * Setup layout spacing.
	 */
	private void setupSpacing() {
		int gap = COMMON_GAP;
		setVgap(gap);
		setHgap(gap);
		setPadding(new Insets(gap));
	}

	/**
	 * Add pane implementations.
	 */
	private void addChildren() {
		ObservableList<Node> children = getChildren();
		children.add(new TitlePane());
		children.add(new DocumentationPane());
		children.add(new GithubPane());
		children.add(new DiscordPane());
		// TODO: Add a panel like eclipse's step-by-step basic configuration
		//  - configure common options like "what do you want to do when dropping a jar file into Recaf?"
	}

	/**
	 * Setup children list so when an item is added all children will be set to the largest width.
	 * Combined with the behavior of {@link FlowPane} this creates a dynamic flow-like grid.
	 */
	private void setupChildSizeFormatter() {
		getChildren().addListener((ListChangeListener<Node>) c -> {
			Threads.runFx(() -> {
				double widestChild = 0.0D;
				for (Node node : getChildren()) {
					if (node instanceof FlowGridItem) {
						double width = node.getBoundsInParent().getWidth();
						if (width > widestChild) {
							widestChild = width;
						}
					}
				}
				for (Node node : getChildren()) {
					if (node instanceof FlowGridItem) {
						// Make all item panes match in width
						node.prefWidth(widestChild);
						node.minWidth(widestChild);
						((Region) node).setPrefWidth(widestChild);
					} else {
						// Fit to screen size
						Region region = (Region) node;
						Region parent = (Region) getParent();
						// Yes the division is necessary to prevent wonky infinite resizing behavior...
						region.prefWidthProperty().bind(parent.widthProperty().divide(1.05));
					}
				}
			});
		});
	}

	/**
	 * Panel to display the current version of Recaf.
	 */
	private static class TitlePane extends BorderPane {
		private TitlePane() {
			Label title = new Label("Recaf " + BuildConfig.VERSION);
			title.getStyleClass().addAll("h1", "b");
			title.setAlignment(Pos.CENTER);
			setCenter(title);
		}
	}

	/**
	 * Common panel base for welcome items.
	 */
	private abstract static class FlowGridItem extends GridPane {
		private static final int H_GAP = COMMON_GAP;

		private FlowGridItem(String iconPath, String titleText, String descriptionText) {
			setHgap(H_GAP);
			IconView image = Icons.getIconView(iconPath, 64);
			Label title = new Label(titleText);
			Label description = new Label(descriptionText);
			title.getStyleClass().addAll("h1", "u");
			image.setOpacity(0.8);
			// Fill column 1, with 2 rows
			add(image, 0, 0, 1, 2);
			// Add to column 2, row 1
			add(title, 1, 0);
			// Add to column 2, row 2
			add(description, 1, 1);
			setOnMouseClicked(e -> {
				if (e.getButton() == MouseButton.PRIMARY) {
					action();
				}
			});
			setOnMouseEntered(e -> enter(image));
			setOnMouseExited(e -> exit(image));
		}

		/**
		 * On-click behavior.
		 */
		protected abstract void action();

		/**
		 * On mouse enter: Make icon glow to indicate selection gained.
		 *
		 * @param image
		 * 		Icon to modify.
		 */
		protected void enter(IconView image) {
			image.setEffect(new Glow(0.8));
		}

		/**
		 * On mouse exit: Remove icon glow to indicate selection lost.
		 *
		 * @param image
		 * 		Icon to modify.
		 */
		protected void exit(IconView image) {
			image.setEffect(null);
		}
	}

	/**
	 * Pane that opens documentation.
	 */
	private static class DocumentationPane extends FlowGridItem {
		private DocumentationPane() {
			super("icons/welcome/documentation.png",
					Lang.get("welcome.documentation.title"),
					Lang.get("welcome.documentation.description"));
		}

		@Override
		protected void action() {
			browse("https://www.coley.software/Recaf-documentation/");
		}
	}

	/**
	 * Pane that opens the github page.
	 */
	private static class GithubPane extends FlowGridItem {
		private GithubPane() {
			super("icons/welcome/github.png",
					Lang.get("welcome.github.title"),
					Lang.get("welcome.github.description"));
		}

		@Override
		protected void action() {
			browse("https://github.com/Col-E/Recaf");
		}
	}

	/**
	 * Pane that opens the discord group.
	 */
	private static class DiscordPane extends FlowGridItem {
		private DiscordPane() {
			super("icons/welcome/discord.png",
					Lang.get("welcome.discord.title"),
					Lang.get("welcome.discord.description"));
		}

		@Override
		protected void action() {
			browse("https://discord.gg/Bya5HaA");
		}
	}

	private static void browse(String uri) {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop desktop = Desktop.getDesktop();
				if (desktop.isSupported(Desktop.Action.BROWSE)) {
					desktop.browse(URI.create(uri));
					return;
				}
			}
			logger.error("Failed to open link, Desktop#browse(URI) unsupported!");
		} catch (IOException ex) {
			logger.error("Failed to open link", ex);
		}
	}
}
