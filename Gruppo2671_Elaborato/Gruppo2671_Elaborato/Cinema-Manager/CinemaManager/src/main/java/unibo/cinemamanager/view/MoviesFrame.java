package unibo.cinemamanager.view;

import unibo.cinemamanager.controller.MovieController;
import unibo.cinemamanager.controller.ReviewController;
import unibo.cinemamanager.Model.Movie;
import unibo.cinemamanager.Model.Review;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.BorderFactory;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;

/**
 * The MoviesFrame class provides a GUI for displaying a list of movies.
 * Users can view movie details and return to the main user frame.
 */
public class MoviesFrame extends JFrame {

    private static final int FRAME_WIDTH = 900;
    private static final int FRAME_HEIGHT = 600;
    private static final int PADDING = 20;
    private static final int INSET = 10;
    private static final int ROW_HEIGHT = 30;
    private static final int STATUS_FONT_SIZE = 12;
    private static final int BUTTON_FONT_SIZE = 14;
    private static final Color SELECTION_BACKGROUND_COLOR = new Color(184, 207, 229);
    private static final Color HEADER_BACKGROUND_COLOR = new Color(200, 200, 200);

    private JTable moviesTable;
    private DefaultTableModel tableModel;
    private JButton backButton;
    private JComboBox<String> reviewFilterComboBox; // ComboBox for filter
    private final UserMainFrame userMainFrame;

    /**
     * Constructor for the MoviesFrame class.
     *
     * @param userMainFrame the main frame to return to when the back button is pressed
     */
    public MoviesFrame(final UserMainFrame userMainFrame) {
        this.userMainFrame = userMainFrame;

        setTitle("View Movies");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setLayout(new BorderLayout(INSET, INSET));
        JPanel mainPanel = new JPanel(new BorderLayout(INSET, INSET));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        mainPanel.setBackground(Color.WHITE);

        // Barra degli strumenti
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.PLAIN, BUTTON_FONT_SIZE));
        toolBar.add(backButton);

        // ComboBox for review filter with three options
        reviewFilterComboBox = new JComboBox<>(new String[]{"All Movies", "Movies with Review > 3", "Movies with Review < 3"});
        reviewFilterComboBox.setFont(new Font("Arial", Font.PLAIN, BUTTON_FONT_SIZE));
        toolBar.add(new JLabel("Filter by Review:"));
        toolBar.add(reviewFilterComboBox);

        mainPanel.add(toolBar, BorderLayout.NORTH);

        // Colonne della tabella
        String[] columnNames = {"ID", "Title", "Description", "Release Date", "Genre", "Duration"};
        tableModel = new DefaultTableModel(columnNames, 0);
        moviesTable = new JTable(tableModel);
        moviesTable.setFillsViewportHeight(true);
        moviesTable.setRowHeight(ROW_HEIGHT);
        moviesTable.setFont(new Font("Arial", Font.PLAIN, BUTTON_FONT_SIZE));
        moviesTable.setSelectionBackground(SELECTION_BACKGROUND_COLOR);
        moviesTable.setSelectionForeground(Color.BLACK);
        moviesTable.setGridColor(Color.LIGHT_GRAY);

        // Personalizza l'intestazione della tabella
        JTableHeader header = moviesTable.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 16));
        header.setBackground(HEADER_BACKGROUND_COLOR);
        header.setForeground(Color.BLACK);
        header.setOpaque(true);
        header.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // Recupera i dati dei film dal database e popolano la tabella
        loadMovies("All Movies"); // Initially load all movies

        JScrollPane scrollPane = new JScrollPane(moviesTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Aggiungi mainPanel al frame
        add(mainPanel);

        // Aggiungi barra di stato
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, STATUS_FONT_SIZE));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(PADDING / 4, PADDING / 4, PADDING / 4, PADDING / 4));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        // Aggiorna barra di stato
        statusLabel.setText("Movies loaded successfully.");

        // Aggiungi azione al pulsante Back
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                dispose();
                userMainFrame.setVisible(true);
            }
        });

        // Action listener for the filter ComboBox
        reviewFilterComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String selectedFilter = (String) reviewFilterComboBox.getSelectedItem();
                loadMovies(selectedFilter); // Load movies based on the selected filter
                if ("Movies with Review > 3".equals(selectedFilter)) {
                    statusLabel.setText("Showing movies with reviews greater than 3.");
                } else if ("Movies with Review < 3".equals(selectedFilter)) {
                    statusLabel.setText("Showing movies with reviews less than 3.");
                } else {
                    statusLabel.setText("Showing all movies.");
                }
            }
        });
    }

    /**
     * Loads movie data from the database and populates the table based on the selected filter.
     *
     * @param filterType the type of filter to apply ("All Movies", "Movies with Review > 3", "Movies with Review < 3")
     */
    private void loadMovies(String filterType) {
        MovieController movieController = new MovieController();
        ReviewController reviewController = new ReviewController();
        try {
            List<Movie> movies;
            if ("Movies with Review > 3".equals(filterType)) {
                // Get movies with an average review score above 3
                List<Review> reviews = reviewController.getMoviesWithReviewsAboveThree();
                movies = movieController.getMoviesByIds(reviews.stream().map(Review::getMovieId).toList());
            } else if ("Movies with Review < 3".equals(filterType)) {
                // Get movies with an average review score below 3
                List<Review> reviews = reviewController.getMoviesWithReviewsBelowThree();
                movies = movieController.getMoviesByIds(reviews.stream().map(Review::getMovieId).toList());
            } else {
                // Get all movies
                movies = movieController.getAllMovies();
            }

            tableModel.setRowCount(0); // Clear existing data
            for (Movie movie : movies) {
                Object[] rowData = {
                        movie.getId(),
                        movie.getTitle(),
                        movie.getDescription(),
                        movie.getReleaseDate(),
                        movie.getGenre(),
                        movie.getDuration()
                };
                tableModel.addRow(rowData);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading movies: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
