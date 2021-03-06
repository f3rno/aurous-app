package me.aurous.utils.playlist;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import me.aurous.player.Settings;
import me.aurous.services.fetchers.impl.YouTubeFetcher;
import me.aurous.services.impl.AurousService;
import me.aurous.services.impl.HateChanService;
import me.aurous.services.impl.RedditService;
import me.aurous.services.impl.YouTubeService;
import me.aurous.swinghacks.GhostText;
import me.aurous.ui.UISession;
import me.aurous.ui.listeners.ContextMenuMouseListener;
import me.aurous.ui.widgets.ExceptionWidget;
import me.aurous.ui.widgets.ImporterWidget;
import me.aurous.utils.Constants;
import me.aurous.utils.Internet;
import me.aurous.utils.ModelUtils;
import me.aurous.utils.Utils;
import me.aurous.utils.media.MediaUtils;

/**
 * @author Andrew
 *
 */
public class PlayListUtils {

	/**
	 * popup panel to add url to playlist
	 */
	public static void additionToPlayListPrompt() {

		if ((Utils.isNull(Settings.getLastPlayList()))
				|| Settings.getLastPlayList().isEmpty()) {
			JOptionPane.showMessageDialog(new JFrame(),
					"You do not have any playlist loaded!", "Uh oh",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		final JTextField urlField = new JTextField();
		urlField.addMouseListener(new ContextMenuMouseListener());
		urlField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {

			}
		});
		new GhostText("https://www.youtube.com/watch?v=TU3b1qyEGsE", urlField);
		urlField.setHorizontalAlignment(SwingConstants.CENTER);
		final JPanel panel = new JPanel(new GridLayout(0, 1));

		panel.add(new JLabel("Paste media url"));
		panel.add(urlField);

		final int result = JOptionPane.showConfirmDialog(null, panel,
				"Add to Playlist", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			addUrlToPlayList(urlField.getText());
		} else {

		}

	}

	public static void addUrlToPlayList(final String url) {
		if (url.isEmpty()) {
			return;
		}
		if (url.contains("&list=")) {
			return;
		} else {
			try {
				final String playListLocation = Settings.getLastPlayList();

				final FileWriter fw = new FileWriter(playListLocation, true); // the
				String data = "";
				// true
				if (url.contains("vk.me/")) {
					data = url;
				} else {
					data = getaddRules(url);
				}

				fw.write("\n" + data);// appends

				fw.close();
				final String playList = Utils.readFile(playListLocation,
						Charset.defaultCharset()); // load altered new playlist
				Utils.writeFile(playList.replaceAll("(?m)^\\s", ""),
						playListLocation); // remove any white space and rewrite
				// it

				ModelUtils.loadPlayList(playListLocation);

			} catch (final IOException ioe) {
				final ExceptionWidget eWidget = new ExceptionWidget(
						Utils.getStackTraceString(ioe, ""));
				eWidget.setVisible(true);
			}
		}

	}

	public static void buildPlayList(final String playListItems,
			final String playListName) {
		final Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					final String name = Constants.DATA_PATH + "playlist/"
							+ playListName + ".plist";
					final String header = "Title,Artist,Time,DateAdded,User,Album,Art,Link";
					final File file = new File(name);

					final PrintWriter printWriter = new PrintWriter(file);
					final String[] lines = playListItems.split("\n");
					printWriter.println(header);
					for (final String line : lines) {
						if (UISession.getBuilderWidget().isOpen()) {

							final String playListItem = MediaUtils
									.getBuiltString(line.trim());
							if (!playListItem.isEmpty()) {

								printWriter.println(playListItem);
							} else {
								continue;
							}

						} else {

							printWriter.close();
							deletePlayList(name);
							return;
						}

					}
					printWriter.close();
					UISession.getBuilderWidget().getLoadingIcon()
							.setVisible(false);
					UISession.getBuilderWidget().getPlayListTextArea()
							.setEditable(true);
					UISession.getBuilderWidget().getBuildListButton()
							.setEnabled(true);
					UISession.getBuilderWidget().getPlayListNameTextField()
							.setEditable(false);

				} catch (final FileNotFoundException e) {
					final ExceptionWidget eWidget = new ExceptionWidget(
							Utils.getStackTraceString(e, ""));
					eWidget.setVisible(true);
				}
			}
		};
		thread.start();

	}

	public static void deletePlayList(final JList<?> list)

	{
		final String path = list.getSelectedValue().toString();
		try {

			final File file = new File(path);

			if (file.delete()) {
				// System.out.println(file.getName() + " is deleted!");
			} else {
				// System.out.println("Delete operation is failed.");
			}

		} catch (final Exception e) {

			final ExceptionWidget eWidget = new ExceptionWidget(
					Utils.getStackTraceString(e, ""));
			eWidget.setVisible(true);

		}
	}

	public static void deletePlayList(final String path)

	{

		try {

			final File file = new File(path);

			if (file.delete()) {
				// System.out.println(file.getName() + " is deleted!");
			} else {
				// System.out.println("Delete operation is failed.");
			}

		} catch (final Exception e) {

			final ExceptionWidget eWidget = new ExceptionWidget(
					Utils.getStackTraceString(e, ""));
			eWidget.setVisible(true);
		}
	}

	public static void disableImporterInterface() {

		final ImporterWidget widget = UISession.getImporterWidget();
		widget.getImportProgressBar().setVisible(true);

		widget.getImportPlayListButton().setEnabled(false);
		widget.getImportInstrucLabel().setText("Importing Playlist");
		widget.getEnterPlaylistLabel().setText("");

	}

	public static String getaddRules(final String sourceURL) {

		if (sourceURL.contains("youtube")) {

			final YouTubeFetcher youTubeFetcher = new YouTubeFetcher(sourceURL,
					Internet.text(sourceURL));

			final String tubeLine = youTubeFetcher.buildLine();
			return tubeLine;

		} else if (sourceURL.contains("soundcloud")) {

			// return SoundCloudGrabber.buildPlayListLine(sourceURL);
		} else {
			JOptionPane.showMessageDialog(null, "No importer found!", "Error",
					JOptionPane.ERROR_MESSAGE);
		}
		return "";
	}

	public static void getImportRules(final String sourceURL,
			final String playListName) {
		if (sourceURL.contains("youtube")) {
			final YouTubeService youTube = new YouTubeService(sourceURL);
			youTube.importPlayList(playListName);

		} else if (sourceURL.contains("soundcloud")) {

		} else if (sourceURL.contains("aurous")) {
			final String shareID = sourceURL.substring(sourceURL
					.lastIndexOf("/") + 1);
			final AurousService aurousService = new AurousService(playListName,
					shareID);
			aurousService.buildPlayList();

		} else if (sourceURL.contains("reddit")) {
			final RedditService redditGrabber = new RedditService(sourceURL,
					playListName);
			redditGrabber.buildPlayList();
		} else if (sourceURL.contains("8chan")) {
			final HateChanService hateChanGrabber = new HateChanService(
					sourceURL, playListName);
			hateChanGrabber.buildPlayList();

		} else {
			JOptionPane.showMessageDialog(null, "No importer found!", "Error",
					JOptionPane.ERROR_MESSAGE);
			UISession.getImporterWidget().getImportProgressBar()
					.setVisible(false);
		}
	}

	/**
	 * popup panel to create a playlist
	 */
	public static String importPlayListPrompt() {
		final JTextField urlField = new JTextField();
		final GhostText gText = new GhostText("Enter service url", urlField);

		urlField.addMouseListener(new ContextMenuMouseListener());
		urlField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {

			}
		});
		urlField.setHorizontalAlignment(SwingConstants.CENTER);
		gText.setHorizontalAlignment(SwingConstants.CENTER);

		final JPanel panel = new JPanel(new GridLayout(0, 1));

		panel.add(new JLabel("Enter a PlayList URL"));
		panel.add(urlField);
		final int result = JOptionPane.showConfirmDialog(null, panel,
				"Add to Service", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			if (!urlField.getText().isEmpty()) {
				return urlField.getText();
			}
		} else {

		}

		return "";

	}

	/**
	 * @author Andrew
	 *
	 *         Deletes line from PlayList
	 */
	public static void removeLineFromPlayList(final String file,
			final String lineToRemove) {

		try {

			final File inFile = new File(file);

			if (!inFile.isFile()) {
				// System.out.println("Parameter is not an existing file");
				return;
			}

			// Construct the new file that will later be renamed to the original
			// filename.
			final File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

			final BufferedReader br = new BufferedReader(new FileReader(file));
			final PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

			String line = null;

			// Read from the original file and write to the new
			// unless content matches data to be removed.
			while ((line = br.readLine()) != null) {

				if (!line.contains(lineToRemove)) {

					pw.println(line);
					pw.flush();
				}
			}
			pw.close();
			br.close();

			// Delete the original file
			if (!inFile.delete()) {
				// System.out.println("Could not delete file");
				return;
			}

			// Rename the new file to the filename the original file had.
			if (!tempFile.renameTo(inFile)) {
				// System.out.println("Could not rename file");
			}
			// loadPlayList(PlayerUtils.currentPlayList);
		} catch (final FileNotFoundException ex) {
			final ExceptionWidget eWidget = new ExceptionWidget(
					Utils.getStackTraceString(ex, ""));
			eWidget.setVisible(true);
		} catch (final IOException ex) {
			final ExceptionWidget eWidget = new ExceptionWidget(
					Utils.getStackTraceString(ex, ""));
			eWidget.setVisible(true);
		}
	}

	/**
	 * @author Andrew
	 *
	 *         Removes a row from the JTable while deleting the line from the
	 *         playlist
	 */
	public static void removeSelectedRows(final JTable table) {
		final DefaultTableModel model = (DefaultTableModel) table.getModel();
		final int[] rows = table.getSelectedRows();
		removeLineFromPlayList(Settings.getLastPlayList(),
				(String) table.getValueAt(rows[0], 7));

		for (int i = 0; i < rows.length; i++) {
			model.removeRow(rows[i] - i);
		}

	}

	public static void resetImporterInterface() {
		final ImporterWidget widget = UISession.getImporterWidget();
		if (widget != null) {
			widget.getImportProgressBar().setValue(0);
			widget.getImportProgressBar().setVisible(false);
			widget.getImportInstrucLabel().setText("Import Playlist");
			widget.getEnterPlaylistLabel().setText("Enter a Playlist Name");
			widget.getImportPlayListButton().setEnabled(true);
		}

	}

	public static String getStats(final String playlist) {
		final String csv = Utils.readFile(playlist, Charset.defaultCharset());
		final Scanner scanner = new Scanner(csv);
		int i = -1;
		long milliseconds = 0;
		while (scanner.hasNextLine()) {
			i++;

			final String line = scanner.nextLine();
			if (i == 0) {
				continue;
			} else {
				final String[] data = line.split("(?<!\\\\),");
				final String[] timeToken = data[2].trim().split(":");
				if (timeToken.length > 2) {
					final int hours = Integer.parseInt(timeToken[0]);
					milliseconds += TimeUnit.MILLISECONDS.convert(hours,
							TimeUnit.HOURS);
					final int minutes = Integer.parseInt(timeToken[1]);
					milliseconds += TimeUnit.MILLISECONDS.convert(minutes,
							TimeUnit.MINUTES);
					final int seconds = Integer.parseInt(timeToken[2]);
					milliseconds += TimeUnit.MILLISECONDS.convert(seconds,
							TimeUnit.SECONDS);

				} else {

					final int minutes = Integer.parseInt(timeToken[0]);

					milliseconds += TimeUnit.MINUTES.toMillis(minutes);
					final int seconds = Integer.parseInt(timeToken[1]);
					milliseconds += TimeUnit.SECONDS.toMillis(seconds);

				}
			}

		}

		scanner.close();

		return String.format("%s songs %s", String.valueOf(i), String
				.valueOf(MediaUtils
						.calculateTime((int) (milliseconds / 1000.0))));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void watchPlayListDirectory(final JList<?> displayList) {

		try {
			final WatchService watcher = FileSystems.getDefault()
					.newWatchService();
			final Path dir = Paths.get(Constants.DATA_PATH + "playlist/");
			dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

			// System.out.println("Watch Service registered for dir: "
			// + dir.getFileName());

			while (true) {
				WatchKey key;
				try {
					key = watcher.take();
				} catch (final InterruptedException ex) {
					return;
				}

				for (final WatchEvent<?> event : key.pollEvents()) {
					final WatchEvent.Kind<?> kind = event.kind();

					java.awt.EventQueue
					// i dont question the Java API, it works now.
							.invokeLater(() -> {

						final DefaultListModel playListModel = new DefaultListModel();

								final File[] playListFolder = new File(
										Constants.DATA_PATH + "playlist/")
										.listFiles();
								if ((kind == ENTRY_CREATE)
										|| ((kind == ENTRY_DELETE)
												&& (playListModel != null) && (playListFolder != null))) {

									for (final File file : playListFolder) {
										playListModel.addElement(file);
									}
									displayList.setModel(playListModel);
							// / displayList.updateUI();
								}
							});

				}

				final boolean valid = key.reset();
				if (!valid) {
					break;
				}
			}

		} catch (final IOException ex) {
			final ExceptionWidget eWidget = new ExceptionWidget(
					Utils.getStackTraceString(ex, ""));
			eWidget.setVisible(true);
		}

	}
}
