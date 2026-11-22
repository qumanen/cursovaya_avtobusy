package buses;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class BusAppGUI extends JFrame {
    private final User currentUser;
    private JTable routesTable;
    private DefaultTableModel routesModel;
    private JTextField searchField;
    private JComboBox<String> sortBox;

    private JTable usersTable;
    private DefaultTableModel usersModel;

    public BusAppGUI(User user) {
        this.currentUser = user;
        String roleTitle = (user.getRole() == 1) ? "Администратор" : "Пользователь";
        setTitle("Автовокзал — Расписание автобусов (" + user.getLogin() + " | " + roleTitle + ")");
        setSize(950, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 14));

        tabs.addTab("Расписание рейсов", createRoutesPanel());

        if (user.getRole() == 1) {
            tabs.addTab("Учетные записи", createUsersPanel());
            tabs.addTab("Управление таблицей БД", createStoragePanel());
        }

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel welcomeLabel = new JLabel("Пользователь: " + user.getLogin() + " (" + roleTitle + ")");
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JButton logoutBtn = new JButton("Сменить пользователя / Выход");
        logoutBtn.addActionListener(e -> {
            dispose();
            showLoginDialog();
        });
        topBar.add(welcomeLabel, BorderLayout.WEST);
        topBar.add(logoutBtn, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        loadRoutesData(null, "id");
        if (user.getRole() == 1) {
            loadUsersData();
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(BusAppGUI::showLoginDialog);
    }

    public static void showLoginDialog() {
        JDialog loginDialog = new JDialog((Frame) null, "Авторизация в системе", true);
        loginDialog.setSize(380, 220);
        loginDialog.setLocationRelativeTo(null);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField loginField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JButton loginBtn = new JButton("Войти");
        JButton exitBtn = new JButton("Выход");

        panel.add(new JLabel("Логин:"));
        panel.add(loginField);
        panel.add(new JLabel("Пароль:"));
        panel.add(passField);
        panel.add(exitBtn);
        panel.add(loginBtn);

        loginBtn.addActionListener(e -> {
            String login = loginField.getText().trim();
            String pass = new String(passField.getPassword()).trim();
            User user = DatabaseManager.authenticate(login, pass);
            if (user != null) {
                loginDialog.dispose();
                new BusAppGUI(user).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(loginDialog,
                        "Неверный логин или пароль!",
                        "Ошибка авторизации", JOptionPane.ERROR_MESSAGE);
            }
        });

        exitBtn.addActionListener(e -> System.exit(0));
        loginDialog.getRootPane().setDefaultButton(loginBtn);
        loginDialog.add(panel);
        loginDialog.setVisible(true);
    }
  
    private JPanel createRoutesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель поиска, сортировки и индивидуального задания
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchField = new JTextField(15);
        String[] sortOptions = {
                "По умолчанию (ID)",
                "По номеру рейса",
                "По типу автобуса",
                "По пункту назначения",
                "По времени отправления",
                "По времени прибытия"
        };
        sortBox = new JComboBox<>(sortOptions);

        JButton applyBtn = new JButton("Найти / Сортировать");
        JButton resetBtn = new JButton("Показать все");
        JButton taskBtn = new JButton("Индивидуальное задание (за 12 ч.)");

        applyBtn.addActionListener(e -> applyFilters());
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            sortBox.setSelectedIndex(0);
            loadRoutesData(null, "id");
        });
        taskBtn.addActionListener(e -> openIndividualTaskDialog());

        filterPanel.add(new JLabel("Поиск:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Сортировка:"));
        filterPanel.add(sortBox);
        filterPanel.add(applyBtn);
        filterPanel.add(resetBtn);
        filterPanel.add(taskBtn);

        // Таблица рейсов
        String[] columns = {"ID", "№ Рейса", "Тип автобуса", "Пункт назначения", "Отправление", "Прибытие"};
        routesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        routesTable = new JTable(routesModel);
        routesTable.setRowHeight(24);
        routesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(routesTable), BorderLayout.CENTER);

        // Нижняя панель редактирования (только для администратора)
        if (currentUser.getRole() == 1) {
            JPanel adminActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            JButton addBtn = new JButton("Добавить рейс");
            JButton editBtn = new JButton("Редактировать выбранный");
            JButton delBtn = new JButton("Удалить выбранный");

            addBtn.addActionListener(e -> openRouteFormDialog(null));
            editBtn.addActionListener(e -> editSelectedRoute());
            delBtn.addActionListener(e -> deleteSelectedRoute());

            adminActions.add(addBtn);
            adminActions.add(editBtn);
            adminActions.add(delBtn);
            panel.add(adminActions, BorderLayout.SOUTH);
        }

        return panel;
    }

    private void applyFilters() {
        String search = searchField.getText().trim();
        String sortField = switch (sortBox.getSelectedIndex()) {
            case 1 -> "route_number";
            case 2 -> "bus_type";
            case 3 -> "destination";
            case 4 -> "departure_time";
            case 5 -> "arrival_time";
            default -> "id";
        };
        loadRoutesData(search, sortField);
    }

    private void loadRoutesData(String search, String sortBy) {
        routesModel.setRowCount(0);
        try {
            List<BusRoute> routes = DatabaseManager.getRoutes(search, sortBy);
            if (routes.isEmpty() && search != null && !search.isEmpty()) {
                JOptionPane.showMessageDialog(this, "По вашему запросу ничего не найдено.",
                        "Результат поиска", JOptionPane.INFORMATION_MESSAGE);
            }
            for (BusRoute r : routes) {
                routesModel.addRow(new Object[]{
                        r.getId(), r.getRouteNumber(), r.getBusType(),
                        r.getDestination(), r.getDepartureTime(), r.getArrivalTime()
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка загрузки данных (возможно, таблица routes не создана): " + e.getMessage(),
                    "Ошибка БД", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openIndividualTaskDialog() {
        JTextField destField = new JTextField("Брест");
        JTextField timeField = new JTextField("23:00");

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Пункт назначения:"));
        form.add(destField);
        form.add(new JLabel("Желаемое время прибытия (ЧЧ:ММ):"));
        form.add(timeField);

        int res = JOptionPane.showConfirmDialog(this, form,
                "Индивидуальное задание (прибытие за 12 ч.)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            String dest = destField.getText().trim();
            String time = timeField.getText().trim();
            if (dest.isEmpty() || time.length() != 5 || time.charAt(2) != ':') {
                JOptionPane.showMessageDialog(this, "Введите город и время строго в формате ЧЧ:ММ (например, 23:00)!",
                        "Ошибка ввода", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                List<BusRoute> list = DatabaseManager.getIndividualTaskRoutes(dest, time);
                routesModel.setRowCount(0);
                if (list.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Рейсов в г. " + dest + ", прибывающих не позднее чем за 12 часов до " + time + ", не найдено.",
                            "Результат поиска", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    for (BusRoute r : list) {
                        routesModel.addRow(new Object[]{
                                r.getId(), r.getRouteNumber(), r.getBusType(),
                                r.getDestination(), r.getDepartureTime(), r.getArrivalTime()
                        });
                    }
                    JOptionPane.showMessageDialog(this,
                            "Найдено подходящих рейсов: " + list.size() + " (таблица отфильтрована).",
                            "Индивидуальное задание выполнено", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка выполнения запроса: " + e.getMessage(),
                        "Ошибка БД", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openRouteFormDialog(BusRoute route) {
        boolean isEdit = (route != null);
        JTextField numF = new JTextField(isEdit ? route.getRouteNumber() : "");
        JTextField typeF = new JTextField(isEdit ? route.getBusType() : "");
        JTextField destF = new JTextField(isEdit ? route.getDestination() : "");
        JTextField depF = new JTextField(isEdit ? route.getDepartureTime().substring(0, 5) : "08:00");
        JTextField arrF = new JTextField(isEdit ? route.getArrivalTime().substring(0, 5) : "12:00");

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("Номер рейса:")); form.add(numF);
        form.add(new JLabel("Тип автобуса:")); form.add(typeF);
        form.add(new JLabel("Пункт назначения:")); form.add(destF);
        form.add(new JLabel("Отправление (ЧЧ:ММ):")); form.add(depF);
        form.add(new JLabel("Прибытие (ЧЧ:ММ):")); form.add(arrF);

        int res = JOptionPane.showConfirmDialog(this, form, isEdit ? "Редактировать" : "Добавить", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                if (isEdit) {
                    DatabaseManager.updateRoute(route.getId(), numF.getText().trim(), typeF.getText().trim(), destF.getText().trim(), depF.getText().trim() + ":00", arrF.getText().trim() + ":00");
                } else {
                    DatabaseManager.addRoute(numF.getText().trim(), typeF.getText().trim(), destF.getText().trim(), depF.getText().trim() + ":00", arrF.getText().trim() + ":00");
                }
                loadRoutesData(null, "id");
                JOptionPane.showMessageDialog(this, "Сохранено!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }
    }
    private void editSelectedRoute() {
        int row = routesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Выберите рейс в таблице!");
            return;
        }
        int id = (int) routesModel.getValueAt(row, 0);
        try {
            openRouteFormDialog(DatabaseManager.getRouteById(id));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }

    private void deleteSelectedRoute() {
        int row = routesTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Выберите рейс в таблице!");
            return;
        }
        int id = (int) routesModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Вы действительно хотите удалить запись?", "Подтверждение", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseManager.deleteRoute(id);
                loadRoutesData(null, "id");
                JOptionPane.showMessageDialog(this, "Запись успешно удалена!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка удаления: " + e.getMessage());
            }
        }
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"ID", "Логин", "Хеш пароля (SHA-256)", "Роль"};
        usersModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = new JTable(usersModel);
        usersTable.setRowHeight(24);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton addBtn = new JButton("Добавить пользователя");
        JButton editBtn = new JButton("Редактировать");
        JButton delBtn = new JButton("Удалить");

        addBtn.addActionListener(e -> openUserDialog(null));
        editBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Выберите пользователя!"); return; }
            int id = (int) usersModel.getValueAt(row, 0);
            String login = (String) usersModel.getValueAt(row, 1);
            int role = usersModel.getValueAt(row, 3).toString().contains("1") ? 1 : 0;
            openUserDialog(new User(id, login, "", role));
        });
        delBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Выберите пользователя!"); return; }
            int id = (int) usersModel.getValueAt(row, 0);
            if (JOptionPane.showConfirmDialog(this, "Вы действительно хотите удалить учетную запись?", "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try {
                    DatabaseManager.deleteUser(id);
                    loadUsersData();
                    JOptionPane.showMessageDialog(this, "Учетная запись удалена!");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
                }
            }
        });

        btnPanel.add(addBtn); btnPanel.add(editBtn); btnPanel.add(delBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }
    private void loadUsersData() {
        usersModel.setRowCount(0);
        try {
            for (User u : DatabaseManager.getAllUsers()) {
                usersModel.addRow(new Object[]{u.getId(), u.getLogin(), u.getPasswordHash(), u.getRole() == 1 ? "Администратор (1)" : "Пользователь (0)"});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка загрузки пользователей: " + e.getMessage());
        }
    }

    private void openUserDialog(User user) {
        boolean isEdit = (user != null);
        JTextField loginF = new JTextField(isEdit ? user.getLogin() : "");
        JTextField passF = new JTextField();
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"0 — Пользователь", "1 — Администратор"});
        if (isEdit) roleBox.setSelectedIndex(user.getRole());

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Логин:")); form.add(loginF);
        form.add(new JLabel(isEdit ? "Новый пароль (необязательно):" : "Пароль:")); form.add(passF);
        form.add(new JLabel("Роль:")); form.add(roleBox);

        if (JOptionPane.showConfirmDialog(this, form, isEdit ? "Редактировать" : "Новый пользователь", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                if (isEdit) {
                    DatabaseManager.updateUser(user.getId(), loginF.getText().trim(), passF.getText().trim(), roleBox.getSelectedIndex());
                } else {
                    DatabaseManager.addUser(loginF.getText().trim(), passF.getText().trim(), roleBox.getSelectedIndex());
                }
                loadUsersData();
                JOptionPane.showMessageDialog(this, "Данные пользователя сохранены!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }
    }

    private JPanel createStoragePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 20));
        JButton createBtn = new JButton("Создать таблицу данных (routes)");
        JButton checkBtn = new JButton("Открыть / проверить данные");
        JButton dropBtn = new JButton("Удалить таблицу данных");

        createBtn.addActionListener(e -> {
            try {
                DatabaseManager.createRoutesTable();
                JOptionPane.showMessageDialog(this, "Таблица данных успешно создана!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
            }
        });
        checkBtn.addActionListener(e -> {
            if (DatabaseManager.isRoutesTableExists()) {
                loadRoutesData(null, "id");
                JOptionPane.showMessageDialog(this, "Таблица данных открыта и обновлена во вкладке расписания.");
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка: таблица данных не создана!", "Внимание", JOptionPane.WARNING_MESSAGE);
            }
        });
        dropBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Вы действительно хотите удалить таблицу данных?", "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try {
                    DatabaseManager.dropRoutesTable();
                    routesModel.setRowCount(0);
                    JOptionPane.showMessageDialog(this, "Таблица данных успешно удалена!");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage());
                }
            }
        });

        panel.add(createBtn); panel.add(checkBtn); panel.add(dropBtn);
        return panel;
    }
}

