module com.example.sudokuia {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.sudokuia to javafx.fxml;
    exports com.example.sudokuia;
}