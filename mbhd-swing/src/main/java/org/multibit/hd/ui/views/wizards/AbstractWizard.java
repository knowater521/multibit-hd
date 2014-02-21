package org.multibit.hd.ui.views.wizards;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.core.events.CoreEvents;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.events.view.LocaleChangedEvent;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.layouts.WizardCardLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Map;

/**
 * <p>Abstract base class to provide the following to UI:</p>
 * <ul>
 * <li>Provision of common methods to wizards</li>
 * </ul>
 *
 * @param <M> the wizard model
 *
 * @since 0.0.1
 */
public abstract class AbstractWizard<M extends WizardModel> {

  private final WizardCardLayout cardLayout;
  private final JPanel wizardPanel;
  private final M wizardModel;

  private final boolean exiting;
  private Map<String, AbstractWizardPanelView> wizardViewMap = Maps.newHashMap();

  /**
   * @param wizardModel The overall wizard data model containing the aggregate information of all components in the wizard
   * @param isExiting   True if the exit button should trigger an application shutdown
   */
  protected AbstractWizard(M wizardModel, boolean isExiting) {

    Preconditions.checkNotNull(wizardModel, "'model' must be present");

    this.wizardModel = wizardModel;
    this.exiting = isExiting;

    CoreServices.uiEventBus.register(this);

    cardLayout = new WizardCardLayout(0, 0);
    wizardPanel = Panels.newPanel(cardLayout);

    // Bind the ESC key to a Cancel/Exit event
    wizardPanel.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "quit");
    if (isExiting) {
      wizardPanel.getActionMap().put("quit", getExitAction());
    } else {
      wizardPanel.getActionMap().put("quit", getCancelAction());
    }

    // Use current locale for initial creation
    onLocaleChangedEvent(new LocaleChangedEvent());

    wizardPanel.setMinimumSize(new Dimension(MultiBitUI.WIZARD_MIN_WIDTH, MultiBitUI.WIZARD_MIN_HEIGHT));
    wizardPanel.setPreferredSize(new Dimension(MultiBitUI.WIZARD_MIN_WIDTH, MultiBitUI.WIZARD_MIN_HEIGHT));

    wizardPanel.setSize(new Dimension(MultiBitUI.WIZARD_MIN_WIDTH, MultiBitUI.WIZARD_MIN_HEIGHT));

    // Show the panel specified by the initial state
    show(wizardModel.getPanelName());

  }

  @Subscribe
  public void onLocaleChangedEvent(LocaleChangedEvent event) {

    Preconditions.checkNotNull(event, "'event' must be present");

    // Clear out any existing components
    wizardPanel.removeAll();

    // Clear out any existing views
    wizardViewMap.clear();

    // Re-populate based on the new locale (could involve an LTR or RTL transition)
    populateWizardViewMap(wizardViewMap);

    // Bind the views into the wizard panel, and share their panel names
    for (Map.Entry<String, AbstractWizardPanelView> entry : wizardViewMap.entrySet()) {

      // Add it to the panel
      wizardPanel.add(entry.getValue().getWizardPanel(), entry.getKey());

    }

    // Once all the views are initialised allow events to occur
    for (Map.Entry<String, AbstractWizardPanelView> entry : wizardViewMap.entrySet()) {

      // Ensure the panel is in the correct starting state
      entry.getValue().fireInitialStateViewEvents();

    }

    // Invalidate for new layout
    Panels.invalidate(wizardPanel);

  }

  /**
   * <p>Add fresh content to the wizard view map</p>
   * <p>The map will be empty whenever this is called</p>
   */
  protected abstract void populateWizardViewMap(Map<String, AbstractWizardPanelView> wizardViewMap);

  /**
   * <p>Hide the wizard</p>
   *
   * @param name The panel name
   */
  public void hide(String name) {

    Preconditions.checkState(wizardViewMap.containsKey(name), "'" + name + "' is not a valid panel name");

    final AbstractWizardPanelView wizardPanelView = wizardViewMap.get(name);

    // Provide warning that the panel is about to be shown
    if (wizardPanelView.beforeHide()) {

      // No abort so hide
      Panels.hideLightBox();

    }


  }

  /**
   * <p>Show the named panel</p>
   *
   * @param name The panel name
   */
  public void show(String name) {

    Preconditions.checkState(wizardViewMap.containsKey(name), "'" + name + "' is not a valid panel name");

    final AbstractWizardPanelView wizardPanelView = wizardViewMap.get(name);

    // Provide warning that the panel is about to be shown
    if (wizardPanelView.beforeShow()) {

      // No abort so show
      cardLayout.show(wizardPanel, name);

      wizardPanelView.afterShow();
    }

  }

  /**
   * @return The wizard panel
   */
  public JPanel getWizardPanel() {
    return wizardPanel;
  }

  /**
   * @return True if the wizard should trigger an "exit" event rather than a "close"
   */
  public boolean isExiting() {
    return exiting;
  }

  /**
   * @return The standard "exit" action to trigger application shutdown
   */
  public Action getExitAction() {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        CoreEvents.fireShutdownEvent();
      }
    };

  }

  /**
   * @return The standard "cancel" action to trigger the removal of the lightbox
   */
  public Action getCancelAction() {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        hide(wizardModel.getPanelName());
      }
    };

  }

  /**
   * @param wizardView The wizard view (providing a reference to its underlying panel model)
   *
   * @return The "finish" action based on the model state
   */
  public <P> Action getFinishAction(final AbstractWizardPanelView<M, P> wizardView) {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        hide(wizardModel.getPanelName());

      }
    };
  }

  /**
   * @param wizardView The wizard view (providing a reference to its underlying panel model)
   *
   * @return The "apply" action based on the model state
   */
  public <P> Action getApplyAction(final AbstractWizardPanelView<M, P> wizardView) {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        hide(wizardModel.getPanelName());

      }
    };
  }

  /**
   * @return The wizard model
   */
  public M getWizardModel() {
    return wizardModel;
  }

  /**
   * @param wizardPanelView The wizard panel view (providing a reference to its underlying panel model)
   *
   * @return The "next" action based on the model state
   */
  public <P> Action getNextAction(final AbstractWizardPanelView<M, P> wizardPanelView) {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        // Ensure the panel updates its model (the button is outside of the panel itself)
        wizardPanelView.updateFromComponentModels(Optional.absent());

        // Move to the next state
        wizardModel.showNext();

        // Show the panel based on the state
        show(wizardModel.getPanelName());
      }
    };
  }

  /**
   * @param wizardView The wizard view (providing a reference to its underlying panel model)
   *
   * @return The "previous" action based on the model state
   */
  public <P> Action getPreviousAction(final AbstractWizardPanelView<M, P> wizardView) {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        // Ensure the panel updates its model (the button is outside of the panel itself)
        wizardView.updateFromComponentModels(Optional.absent());

        // Aggregate the panel information into the wizard model

        // Move to the previous state
        wizardModel.showPrevious();

        // Show the panel based on the state
        show(wizardModel.getPanelName());
      }
    };
  }

}
