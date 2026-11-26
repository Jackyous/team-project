package app;

import data_access.FileUserDataAccessObject;
import data_access.InMemoryPetDataAccessObject;
import data_access.GoogleDesktopIdentityGateway;
import data_access.FileBudgetDataAccessObject;

import data_access.ApiPetImageDataAccessObject;
import entity.UserFactory;

import interface_adapter.ViewManagerModel;
import interface_adapter.google_login.GoogleLoginController;
import interface_adapter.logged_in.ChangePasswordController;
import interface_adapter.logged_in.ChangePasswordPresenter;
import interface_adapter.logged_in.LoggedInViewModel;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.logout.LogoutController;
import interface_adapter.logout.LogoutPresenter;
import interface_adapter.record_transaction.RecordTransactionController;
import interface_adapter.record_transaction.RecordTransactionPresenter;
import interface_adapter.record_transaction.RecordTransactionState;
import interface_adapter.record_transaction.RecordTransactionViewModel;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import interface_adapter.view_budget_progress.ViewBudgetProgressController;
import interface_adapter.view_budget_progress.ViewBudgetProgressPresenter;
import interface_adapter.view_budget_progress.ViewBudgetProgressViewModel;
import interface_adapter.set_budget.SetBudgetController;
import interface_adapter.set_budget.SetBudgetPresenter;
import interface_adapter.set_budget.SetBudgetState;
import interface_adapter.set_budget.SetBudgetViewModel;

import interface_adapter.adopt_pet.AdoptPetViewModel;
import interface_adapter.interact_with_pet.InteractWithPetController;
import interface_adapter.interact_with_pet.InteractWithPetPresenter;
import interface_adapter.interact_with_pet.InteractWithPetViewModel;
import interface_adapter.get_pet_picture.GetPetPictureController;
import interface_adapter.get_pet_picture.GetPetPicturePresenter;
import interface_adapter.adopt_pet.AdoptPetController;
import interface_adapter.adopt_pet.AdoptPetPresenter;
import use_case.change_password.ChangePasswordInputBoundary;
import use_case.change_password.ChangePasswordInteractor;
import use_case.change_password.ChangePasswordOutputBoundary;
import use_case.google_login.GoogleLoginInteractor;
import use_case.google_login.GoogleIdentityGateway;
import use_case.login.LoginInputBoundary;
import use_case.login.LoginInteractor;
import use_case.login.LoginOutputBoundary;
import use_case.logout.LogoutInputBoundary;
import use_case.logout.LogoutInteractor;
import use_case.logout.LogoutOutputBoundary;
import use_case.record_transaction.RecordTransactionInputBoundary;
import use_case.record_transaction.RecordTransactionInteractor;
import use_case.record_transaction.RecordTransactionOutputBoundary;
import use_case.signup.SignupInputBoundary;
import use_case.signup.SignupInteractor;
import use_case.signup.SignupOutputBoundary;
import use_case.view_budget_progress.ViewBudgetProgressInteractor;
import use_case.set_budget.SetBudgetInputBoundary;
import use_case.set_budget.SetBudgetInteractor;
import use_case.set_budget.SetBudgetOutputBoundary;

import use_case.interact_with_pet.InteractWithPetInteractor;
import use_case.get_pet_picture.GetPetPictureInteractor;
import use_case.get_pet_picture.GetPetPictureOutputBoundary;
import use_case.adopt_pet.AdoptPetInteractor;
import view.LoggedInView;
import view.LoginView;
import view.RecordTransactionView;
import view.SignupView;
import view.ViewManager;
import view.ViewBudgetProgressView;
import view.AdoptPetView;
import view.InteractWithPetView;
import view.SetBudgetView;


import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;

import data_access.EarnRewardCoinsDataAccessObject;
import entity.Wallet;

import interface_adapter.earn_reward_coins.EarnRewardCoinsController;
import interface_adapter.earn_reward_coins.EarnRewardCoinsPresenter;
import interface_adapter.earn_reward_coins.EarnRewardCoinsViewModel;

import use_case.earn_reward_coins.EarnRewardCoinsInteractor;
import use_case.earn_reward_coins.EarnRewardCoinsStatusInteractor;

import view.EarnRewardCoinsView;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import data_access.FileTransactionDataAccessObject;

public class AppBuilder {
    private final JPanel cardPanel = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    final UserFactory userFactory = new UserFactory();
    final ViewManagerModel viewManagerModel = new ViewManagerModel();
    ViewManager viewManager = new ViewManager(cardPanel, cardLayout, viewManagerModel);
    final FileUserDataAccessObject userDataAccessObject = new FileUserDataAccessObject(
            "users.csv", userFactory);

    private final FileBudgetDataAccessObject budgetDataAccessObject =
            new FileBudgetDataAccessObject("budgets.json");

    private final FileTransactionDataAccessObject transactionDataAccessObject =
            new FileTransactionDataAccessObject(
                    userDataAccessObject,
                    budgetDataAccessObject);

    private final Clock applicationClock = Clock.systemDefaultZone();

    private final EarnRewardCoinsDataAccessObject rewardDataAccessObject =
            new EarnRewardCoinsDataAccessObject(
                    transactionDataAccessObject,
                    budgetDataAccessObject);

    private SignupView signupView;
    private SignupViewModel signupViewModel;
    private LoginViewModel loginViewModel;
    private LoggedInViewModel loggedInViewModel;
    private LoggedInView loggedInView;
    private LoginView loginView;
    private SwingWorker<String, Void> googleLoginWorker;
    private final InMemoryPetDataAccessObject petDataAccessObject =
            new InMemoryPetDataAccessObject();

    private AdoptPetViewModel adoptPetViewModel;
    private InteractWithPetViewModel interactWithPetViewModel;

    private AdoptPetView adoptPetView;
    private InteractWithPetView interactWithPetView;

    private RecordTransactionView recordTransactionView;
    private RecordTransactionViewModel recordTransactionViewModel;

    private ViewBudgetProgressViewModel budgetProgressViewModel;
    private ViewBudgetProgressView budgetProgressView;

    private SetBudgetViewModel setBudgetViewModel;
    private SetBudgetView setBudgetView;

    public AppBuilder() {
        view.settings.ThemeManager.initialize();

        // cardPanel is created before this constructor runs.
        SwingUtilities.updateComponentTreeUI(cardPanel);

        cardPanel.setLayout(cardLayout);

    }

    public AppBuilder addSignupView() {
        signupViewModel = new SignupViewModel();
        signupView = new SignupView(signupViewModel);
        cardPanel.add(signupView, SignupViewModel.VIEW_NAME);
        return this;
    }

    public AppBuilder addLoginView() {
        loginViewModel = new LoginViewModel();
        loginView = new LoginView(loginViewModel);
        cardPanel.add(loginView, LoginViewModel.VIEW_NAME);
        return this;
    }

    public AppBuilder addLoggedInView() {
        loggedInViewModel = new LoggedInViewModel();
        loggedInView = new LoggedInView(loggedInViewModel);
        cardPanel.add(loggedInView, LoggedInViewModel.VIEW_NAME);
        return this;
    }

    public AppBuilder addAdoptPetView() {
        adoptPetViewModel = new AdoptPetViewModel();
        adoptPetView = new AdoptPetView(adoptPetViewModel);

        cardPanel.add(adoptPetView, AdoptPetViewModel.VIEW_NAME);
        return this;
    }

    public AppBuilder addInteractWithPetView() {
        interactWithPetViewModel = new InteractWithPetViewModel();
        interactWithPetView =
                new InteractWithPetView(interactWithPetViewModel);

        cardPanel.add(
                interactWithPetView,
                InteractWithPetViewModel.VIEW_NAME
        );
        return this;
    }

    public AppBuilder addInteractWithPetUseCase() {
        InteractWithPetPresenter presenter =
                new InteractWithPetPresenter(
                        adoptPetViewModel,
                        interactWithPetViewModel,
                        viewManagerModel
                );

        InteractWithPetInteractor interactor =
                new InteractWithPetInteractor(
                        petDataAccessObject,
                        presenter
                );

        InteractWithPetController controller =
                new InteractWithPetController(interactor);

        loggedInView.setInteractWithPetController(controller);
        return this;
    }

    public AppBuilder addSignupUseCase() {
        final SignupOutputBoundary signupOutputBoundary = new SignupPresenter(viewManagerModel,
                signupViewModel, loginViewModel);
        final SignupInputBoundary userSignupInteractor = new SignupInteractor(
                userDataAccessObject, signupOutputBoundary, userFactory);

        SignupController controller = new SignupController(userSignupInteractor);
        signupView.setSignupController(controller);
        return this;
    }

    public AppBuilder addLoginUseCase() {
        final LoginOutputBoundary loginOutputBoundary = new LoginPresenter(viewManagerModel,
                loggedInViewModel, loginViewModel);
        final LoginInputBoundary loginInteractor = new LoginInteractor(
                userDataAccessObject, loginOutputBoundary);

        LoginController loginController = new LoginController(loginInteractor);
        loginView.setLoginController(loginController);
        loginView.setShowSignup(() -> {
            viewManagerModel.setState(SignupViewModel.VIEW_NAME);
            viewManagerModel.notifyViewChanged();
        });
        return this;
    }

    public AppBuilder addChangePasswordUseCase() {
        final ChangePasswordOutputBoundary changePasswordOutputBoundary = new ChangePasswordPresenter(loggedInViewModel);

        final ChangePasswordInputBoundary changePasswordInteractor =
                new ChangePasswordInteractor(userDataAccessObject, changePasswordOutputBoundary, userFactory);

        ChangePasswordController changePasswordController = new ChangePasswordController(changePasswordInteractor);
        loggedInView.setChangePasswordController(changePasswordController);
        return this;
    }

    /**
     * Adds the Logout Use Case to the application.
     * @return this builder
     */
    public AppBuilder addLogoutUseCase() {
        final LogoutOutputBoundary logoutOutputBoundary = new LogoutPresenter(viewManagerModel,
                loggedInViewModel, loginViewModel);

        final LogoutInputBoundary logoutInteractor =
                new LogoutInteractor(userDataAccessObject, logoutOutputBoundary);

        final LogoutController logoutController = new LogoutController(logoutInteractor);
        loggedInView.setLogoutController(logoutController);
        return this;
    }

    public AppBuilder addGoogleLoginUseCase() {
        String clientId = System.getenv("GOOGLE_OAUTH_CLIENT_ID");
        String clientSecret = System.getenv("GOOGLE_OAUTH_CLIENT_SECRET");
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return this;
        }

        return addGoogleLoginUseCase(new GoogleDesktopIdentityGateway(clientId, clientSecret));
    }

    // Package visibility allows tests to supply an identity gateway without a browser.
    AppBuilder addGoogleLoginUseCase(GoogleIdentityGateway gateway) {
        LoginPresenter presenter = new LoginPresenter(viewManagerModel, loggedInViewModel, loginViewModel);
        loginView.setGoogleLoginCancelAction(() -> {
            SwingWorker<String, Void> pending = googleLoginWorker;
            if (pending != null) {
                // Invalidate first: an already queued completion must not start a session.
                googleLoginWorker = null;
                pending.cancel(true);
                loginView.setGoogleLoginPending(false);
                presenter.prepareFailView("Google sign-in cancelled. You can try again.");
            }
        });
        loginView.setGoogleLoginAction(() -> {
            if (googleLoginWorker != null) {
                return;
            }
            loginView.setGoogleLoginPending(true);
            googleLoginWorker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return gateway.authenticate();
                }

                @Override
                protected void done() {
                    if (googleLoginWorker != this) {
                        return;
                    }
                    googleLoginWorker = null;
                    loginView.setGoogleLoginPending(false);
                    try {
                        String subject = get();
                        // Commit the session on the EDT, serialized with cancellation.
                        new GoogleLoginController(new GoogleLoginInteractor(
                                () -> subject, userDataAccessObject, presenter)).execute();
                    }
                    catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        presenter.prepareFailView("Google sign-in was interrupted.");
                    }
                    catch (ExecutionException exception) {
                        Throwable cause = exception.getCause();
                        String message = cause instanceof java.io.IOException
                                ? cause.getMessage() : "Google sign-in could not finish. Try again.";
                        presenter.prepareFailView(message);
                    }
                }
            };
            googleLoginWorker.execute();
        });
        return this;
    }

    public AppBuilder addRecordTransactionView() {
        recordTransactionViewModel = new RecordTransactionViewModel();
        recordTransactionView = new RecordTransactionView(recordTransactionViewModel);

        cardPanel.add(recordTransactionView, RecordTransactionViewModel.VIEW_NAME);
        return this;
    }

    public AppBuilder addRecordTransactionUseCase() {
        final RecordTransactionOutputBoundary presenter =
                new RecordTransactionPresenter(recordTransactionViewModel);

        final RecordTransactionInputBoundary interactor =
                new RecordTransactionInteractor(transactionDataAccessObject, presenter);

        final RecordTransactionController controller =
                new RecordTransactionController(interactor);

        recordTransactionView.setRecordTransactionController(controller);

        loggedInView.setShowRecordTransaction(() -> {
            // Start with a fresh form whenever this screen opens.
            recordTransactionViewModel.setState(new RecordTransactionState());
            recordTransactionViewModel.notifyStateChanged();

            viewManagerModel.setState(RecordTransactionViewModel.VIEW_NAME);
            viewManagerModel.notifyViewChanged();
        });

        recordTransactionView.setBackAction(() -> {
            viewManagerModel.setState(LoggedInViewModel.VIEW_NAME);
            viewManagerModel.notifyViewChanged();
        });

        recordTransactionView.setBackAction(() -> {
            refreshBankSummary();

            viewManagerModel.setState(LoggedInViewModel.VIEW_NAME);
            viewManagerModel.notifyViewChanged();
        });

        return this;
    }

    public AppBuilder addViewBudgetProgressView() {
        budgetProgressViewModel = new ViewBudgetProgressViewModel();
        budgetProgressView = new ViewBudgetProgressView(budgetProgressViewModel);
        cardPanel.add(budgetProgressView, ViewBudgetProgressViewModel.VIEW_NAME);
        return this;
    }

    public AppBuilder addViewBudgetProgressUseCase() {
        final ViewBudgetProgressPresenter presenter =
                new ViewBudgetProgressPresenter(budgetProgressViewModel);
        final ViewBudgetProgressInteractor interactor =
                new ViewBudgetProgressInteractor(
                        transactionDataAccessObject, budgetDataAccessObject, presenter);
        final ViewBudgetProgressController controller =
                new ViewBudgetProgressController(interactor);
        budgetProgressView.setController(controller);

        loggedInView.setShowBudgetProgress(() -> {
            controller.execute();
            viewManagerModel.setState(ViewBudgetProgressViewModel.VIEW_NAME);
            viewManagerModel.notifyViewChanged();
        });
        budgetProgressView.setBackAction(() -> {
            viewManagerModel.setState(LoggedInViewModel.VIEW_NAME);
            viewManagerModel.notifyViewChanged();
        });
        return this;
        }

    public AppBuilder addGetPetPictureUseCase() {
        ApiPetImageDataAccessObject imageDataAccess =
                new ApiPetImageDataAccessObject();

        GetPetPicturePresenter presenter =
                new GetPetPicturePresenter(adoptPetViewModel);

        GetPetPictureOutputBoundary uiOutput =
                new GetPetPictureOutputBoundary() {
                    @Override
                    public void prepareSuccessView(String imageUrl) {
                        SwingUtilities.invokeLater(
                                () -> presenter.prepareSuccessView(imageUrl)
                        );
                    }

                    @Override
                    public void prepareFailView(String errorMessage) {
                        SwingUtilities.invokeLater(
                                () -> presenter.prepareFailView(errorMessage)
                        );
                    }
                };

        GetPetPictureInteractor interactor =
                new GetPetPictureInteractor(imageDataAccess, uiOutput);

        GetPetPictureController controller =
                new GetPetPictureController(interactor);

        adoptPetView.setGetPetPictureController(controller);

        return this;
    }

    public AppBuilder addAdoptPetUseCase() {
        AdoptPetPresenter presenter = new AdoptPetPresenter(
                adoptPetViewModel,
                interactWithPetViewModel,
                viewManagerModel
        );

        AdoptPetInteractor interactor = new AdoptPetInteractor(
                petDataAccessObject,
                presenter
        );

        AdoptPetController controller = new AdoptPetController(interactor);
        adoptPetView.setAdoptPetController(controller);

        return this;
    }

        public AppBuilder addSetBudgetView() {
        setBudgetViewModel = new SetBudgetViewModel();
        setBudgetView = new SetBudgetView(setBudgetViewModel);
        cardPanel.add(setBudgetView, SetBudgetViewModel.VIEW_NAME);
        return this;
    }

    public AppBuilder addSetBudgetUseCase() {
        final SetBudgetOutputBoundary setBudgetOutputBoundary = new SetBudgetPresenter(
                viewManagerModel, setBudgetViewModel, loggedInViewModel);
        final SetBudgetInputBoundary setBudgetInteractor = new SetBudgetInteractor(
                budgetDataAccessObject, setBudgetOutputBoundary);

        final SetBudgetController controller = new SetBudgetController(setBudgetInteractor);
        setBudgetView.setSetBudgetController(controller);

        loggedInView.setShowSetBudget(() -> {
            String username = loggedInViewModel.getState().getUsername();

            SetBudgetState state = new SetBudgetState();
            state.setUsername(username);

            Wallet wallet = budgetDataAccessObject.getWallet(username);
            state.setBalanceInitialized(wallet != null);

            if (wallet != null) {
                // Balance setup is complete; no starting amount is requested.
                state.setBalance("");
            }

            setBudgetViewModel.setState(state);
            setBudgetViewModel.notifyStateChanged();

            viewManagerModel.setState(SetBudgetViewModel.VIEW_NAME);
            viewManagerModel.notifyViewChanged();
        });

        setBudgetView.setShowLoggedIn(() -> {
            refreshBankSummary();

            viewManagerModel.setState(LoggedInViewModel.VIEW_NAME);
            viewManagerModel.notifyViewChanged();
        });
        return this;
    }

    public JFrame build() {
        final JFrame application = new JFrame("My Virtual Pet");
        application.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        application.add(cardPanel);
        //prevents the previous account’s reward status from appearing for another user.
        if (loggedInView != null) {
            installBankActions(application);
        }

        viewManagerModel.setState(LoginViewModel.VIEW_NAME);
        viewManagerModel.notifyViewChanged();

        view.settings.SettingsMenu.install(application);

        return application;
    }

    private void refreshBankSummary() {
        String username = userDataAccessObject.getCurrentUsername();

        if (username == null || username.isBlank()) {
            return;
        }

        ZoneId zone = applicationClock.getZone();

        rewardDataAccessObject.ensureUser(username, zone);

        // Convert an old undated budget only if this user needs migration.
        budgetDataAccessObject.migrateLegacyBudget(
                username, LocalDate.now(applicationClock));

        Wallet wallet = budgetDataAccessObject.getWallet(username);
        BigDecimal moneyBalance = null;

        if (wallet != null) {
            moneyBalance = wallet.getBalance();
        }

        loggedInView.updateBankSummary(
                moneyBalance,
                rewardDataAccessObject.getCoinBalance(username));
    }

    private void installBankActions(JFrame application) {
        loggedInView.setShowBank(this::refreshBankSummary);

        loggedInView.setShowRewards(() -> {
            String username = userDataAccessObject.getCurrentUsername();

            if (username == null || username.isBlank()) {
                return;
            }

            refreshBankSummary();

            EarnRewardCoinsViewModel viewModel =
                    new EarnRewardCoinsViewModel();

            EarnRewardCoinsPresenter presenter =
                    new EarnRewardCoinsPresenter(viewModel);

            EarnRewardCoinsInteractor claimInteractor =
                    new EarnRewardCoinsInteractor(
                            rewardDataAccessObject,
                            presenter,
                            10,
                            applicationClock);

            EarnRewardCoinsStatusInteractor statusInteractor =
                    new EarnRewardCoinsStatusInteractor(
                            rewardDataAccessObject,
                            presenter,
                            applicationClock);

            EarnRewardCoinsController controller =
                    new EarnRewardCoinsController(
                            claimInteractor, statusInteractor);

            EarnRewardCoinsView rewardView =
                    new EarnRewardCoinsView(
                            username, controller, viewModel);

            JDialog dialog = new JDialog(
                    application, "Reward Coins", true);

            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setContentPane(rewardView);
            dialog.pack();
            dialog.setLocationRelativeTo(application);

            // Modal: execution resumes here after the dialog closes.
            dialog.setVisible(true);

            refreshBankSummary();
        });
    }
}
