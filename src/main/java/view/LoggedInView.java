package view;

import interface_adapter.logged_in.ChangePasswordController;
import interface_adapter.logged_in.LoggedInState;
import interface_adapter.logged_in.LoggedInViewModel;
import interface_adapter.logged_in.LoggedInViewModelListener;
import interface_adapter.logout.LogoutController;
import interface_adapter.interact_with_pet.InteractWithPetController;
import interface_adapter.open_pet_shop.OpenPetShopController;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The View for when the user is logged into the program.
 */
public class LoggedInView extends JPanel implements ActionListener, LoggedInViewModelListener {

    private final LoggedInViewModel loggedInViewModel;
    private final JLabel passwordErrorField = new JLabel();
    private ChangePasswordController changePasswordController = null;
    private LogoutController logoutController;
    private InteractWithPetController interactWithPetController;
    private OpenPetShopController openPetShopController;

    private final JLabel username;

    private final JButton logOut;

    private final JPasswordField passwordInputField = new JPasswordField(15);
    private final JButton changePassword;

    private final JButton recordTransaction = new JButton("Record Transaction");

    private final JButton viewBudgetProgress = new JButton("View Budget Progress");

    private final JButton setBudget = new JButton("Set Budget");

    private final JButton bankSystem = new JButton("Bank system");
    private final JButton pet = new JButton("Pet");

    private final JPanel bankPanel = new JPanel();
    private final JPanel petPanel = new JPanel();

    private final JLabel moneyBalanceLabel =
            new JLabel("Money balance: not loaded");

    private final JLabel rewardCoinsLabel =
            new JLabel("Reward coins: not loaded");

    private final JButton rewards =
            new JButton("View / Claim Reward");

    private Runnable showBank = () -> { };

    public LoggedInView(LoggedInViewModel loggedInViewModel) {
        this.loggedInViewModel = loggedInViewModel;
        this.loggedInViewModel.addListener(this);

        final JLabel title = new JLabel("Logged In Screen");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        final LabelTextPanel passwordInfo = new LabelTextPanel(
                new JLabel("Password"), passwordInputField);

        final JLabel usernameInfo = new JLabel("Currently logged in: ");
        username = new JLabel();

        final JPanel buttons = new JPanel();
        logOut = new JButton("Log Out");
        buttons.add(logOut);

        changePassword = new JButton("Change Password");
        buttons.add(changePassword);
        buttons.add(bankSystem);
        buttons.add(pet);

        logOut.addActionListener(this);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        passwordInputField.getDocument().addDocumentListener(new DocumentListener() {

            private void documentListenerHelper() {
                final LoggedInState currentState = loggedInViewModel.getState();
                currentState.setPassword(new String(passwordInputField.getPassword()));
                loggedInViewModel.setState(currentState);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                documentListenerHelper();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                documentListenerHelper();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                documentListenerHelper();
            }
        });

        changePassword.addActionListener(
                // This creates an anonymous subclass of ActionListener and instantiates it.
                evt -> {
                    if (evt.getSource().equals(changePassword)) {
                        final LoggedInState currentState = loggedInViewModel.getState();

                        this.changePasswordController.execute(
                                currentState.getUsername(),
                                currentState.getPassword()
                        );
                    }
                }
        );

        this.add(title);
        this.add(usernameInfo);
        this.add(username);

        this.add(passwordInfo);
        this.add(passwordErrorField);
        this.add(buttons);

        bankPanel.setLayout(new BoxLayout(bankPanel, BoxLayout.Y_AXIS));

        bankPanel.add(moneyBalanceLabel);
        bankPanel.add(rewardCoinsLabel);

        petShop.addActionListener(event -> {
            String currentUsername =
                    loggedInViewModel.getState().getUsername();

            openPetShopController.execute(currentUsername);
        });

        interactPet.addActionListener(evt -> {
            String currentUsername =
                    loggedInViewModel.getState().getUsername();

            interactWithPetController.execute(currentUsername);
        });
      
      JPanel bankActions = new JPanel();
        bankActions.add(recordTransaction);
        bankActions.add(viewBudgetProgress);
        bankActions.add(setBudget);
        bankActions.add(rewards);

        bankPanel.add(bankActions);
        bankPanel.setVisible(false);

// Enable when AppBuilder connects the reward screen.
        rewards.setEnabled(false);

        JButton interactPet = new JButton("Interact with Pet");
        JButton petShop = new JButton("Pet Item Shop");

// Your current shop button has no action connected.
        petShop.setEnabled(false);
        petShop.setToolTipText("Pet shop is not connected yet.");

        petPanel.add(interactPet);
        petPanel.add(petShop);
        petPanel.setVisible(false);

        bankSystem.addActionListener(event -> {
            bankPanel.setVisible(true);
            petPanel.setVisible(false);

            showBank.run();

            revalidate();
            repaint();
        });

        pet.addActionListener(event -> {
            bankPanel.setVisible(false);
            petPanel.setVisible(true);

            revalidate();
            repaint();
        });

        this.add(bankPanel);
        this.add(petPanel);


    }

    /**
     * React to a button click that results in evt.
     * @param evt the ActionEvent to react to
     */
    public void actionPerformed(ActionEvent evt) {
        logoutController.execute();
    }

    public void setChangePasswordController(ChangePasswordController changePasswordController) {
        this.changePasswordController = changePasswordController;
    }

    public void setLogoutController(LogoutController logoutController) {
        this.logoutController = logoutController;
    }

    public void setShowRecordTransaction(Runnable action) {
        recordTransaction.addActionListener(event -> action.run());
    }

    public void setShowBudgetProgress(Runnable action) {
        viewBudgetProgress.addActionListener(event -> action.run());
    }

    public void setShowSetBudget(Runnable action) {                 
        setBudget.addActionListener(event -> action.run());
    }

    public void setInteractWithPetController(
            InteractWithPetController interactWithPetController) {
        this.interactWithPetController = interactWithPetController;
    }

    public void setOpenPetShopController(
            OpenPetShopController openPetShopController) {
        this.openPetShopController = openPetShopController;
    }

    /**
     * The logged-in state was updated.
     */
    @Override
    public void stateChanged() {
        LoggedInState state = loggedInViewModel.getState();
        final boolean googleAccount = state.getUsername().startsWith("google:");
        username.setText(googleAccount ? "Google account" : state.getUsername());
        changePassword.setEnabled(!googleAccount);
        passwordInputField.setEnabled(!googleAccount);
        passwordInputField.setText("");
        bankPanel.setVisible(false);
        petPanel.setVisible(false);
        moneyBalanceLabel.setText("Money balance: not loaded");
        rewardCoinsLabel.setText("Reward coins: not loaded");
    }

    /**
     * A password change attempt occurred.
     */
    @Override
    public void passwordChanged() {
        LoggedInState state = loggedInViewModel.getState();
        if (state.getPasswordError() == null) {
            JOptionPane.showMessageDialog(this, "password updated for " + state.getUsername());
            passwordInputField.setText("");
        }
        else {
            JOptionPane.showMessageDialog(this, state.getPasswordError());
        }
    }

    public void setShowBank(Runnable action) {
        showBank = action;
    }

    public void setShowRewards(Runnable action) {
        rewards.addActionListener(event -> action.run());
        rewards.setEnabled(true);
    }

    public void updateBankSummary(
            java.math.BigDecimal moneyBalance, int rewardCoins) {

        moneyBalanceLabel.setText(
                moneyBalance == null
                        ? "Money balance: set your starting balance first"
                        : "Money balance: $" + moneyBalance.toPlainString());

        rewardCoinsLabel.setText("Reward coins: " + rewardCoins);
    }
}
