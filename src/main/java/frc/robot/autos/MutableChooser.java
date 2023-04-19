package frc.robot.autos;

import edu.wpi.first.networktables.NTSendable;
import edu.wpi.first.networktables.NTSendableBuilder;
import edu.wpi.first.util.sendable.SendableRegistry;

import edu.wpi.first.networktables.StringTopic;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.networktables.IntegerTopic;
import edu.wpi.first.networktables.IntegerPublisher;

import java.util.LinkedHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import frc.robot.shuffleboard.ShuffleboardManager;

/** A {@link edu.wpi.first.wpilibj.smartdashboard.SendableChooser SendableChooser}-like class allowing for the removal of options. */
public class MutableChooser<V extends Enum<V> & AutoSelector.Named> implements NTSendable, AutoCloseable {
  /** The key for the default value. */
  private static final String DEFAULT = "default";
  /** The key for the selected option. */
  private static final String SELECTED = "selected";
  /** The key for the active option. */
  private static final String ACTIVE = "active";
  /** The key for the option array. */
  private static final String OPTIONS = "options";
  /** The key for the instance number. */
  private static final String INSTANCE = ".instance";

  /** A Lock to stop simultaneous editing of shuffleboard options, selection, or selection publishers. */
  private final ReentrantLock m_networkLock = new ReentrantLock(true);
  /** A map linking options to their identifiers, for use with shuffleboard. */
  private final LinkedHashMap<String, V> m_linkedOptions = new LinkedHashMap<>();
  /** Key for the default selection. */
  private final String m_defaultKey;
  /** Default selection. */
  private final V m_defaultObj;
  /** Key for the current selection. */
  private String m_selectedKey;

  /** A Lock to stop simulataneous reading and writing to list of updates. */
  private final ReentrantLock m_updateLock = new ReentrantLock(true);
  /** A Set storing the options to be updated on the next update. */
  private final EnumSet<V> m_optionsWanted;
  /** If an update is required to sync options on shuffleboard. */
  private boolean m_updateReq = false;

  /** A consumer to be called with the old and new selections when the selection changes. */
  private BiConsumer<V, V> m_bindTo = null;

  /** ArrayList to keep track of publishers. */
  private final ArrayList<StringPublisher> m_activePubs = new ArrayList<>();

  /** Number of instances. */
  private final int m_instance;
  private static int s_instances = 0;

  /**
   * Instantiates a new Chooser with a default option.
   *
   * @param obj the default option
   */
  MutableChooser(V obj) {
    m_instance = s_instances++;
    SendableRegistry.add(this, "SendableChooser", m_instance);

    m_defaultKey = obj.getName();
    m_defaultObj = obj;
    m_selectedKey = m_defaultKey;

    m_optionsWanted = EnumSet.noneOf(obj.getDeclaringClass());
    m_linkedOptions.put(m_defaultKey, obj);
  }

  private void updateOptions() {
    ShuffleboardManager.getInstance().updateChoosersSynced();
    m_networkLock.lock();
    m_updateLock.lock();
    try {
      if (!m_selectedKey.equals(m_defaultKey)) {return;}

      m_linkedOptions.clear();
      m_linkedOptions.put(m_defaultKey, m_defaultObj);
      m_optionsWanted.forEach(e -> m_linkedOptions.put(e.getName(), e));
      m_updateReq = false;
    } finally {
      m_networkLock.unlock();
      m_updateLock.unlock();
    }
    ShuffleboardManager.getInstance().updateChoosersSynced();
  }

  /**
   * Adds an option to the chooser. Takes affect when the selected option is the default.
   * Attempting to add duplicate options will do nothing.
   *
   * @param option the option to add
   */
  public void add(V option) {
    m_updateLock.lock();
    try {
      if (m_optionsWanted.add(option)) {
        m_updateReq = true;
        updateOptions();
      }
    } finally {
      m_updateLock.unlock();
    }
  }

  /**
   * Removes an option from the chooser. Takes affect when the selected option is the default.
   * Attempting to remove the default option will do nothing.
   *
   * @param option the option to remove
   */
  public void remove(V option) {
    m_updateLock.lock();
    try {
      if (m_optionsWanted.remove(option)) {
        m_updateReq = true;
        updateOptions();
      }
    } finally {
      m_updateLock.unlock();
    }
  }

  /**
   * Sets all options in the chooser (not including the default).
   * Takes affect when the selected option is the default.
   *
   * @param options the options to be presented
   */
  @SafeVarargs
  public final void setAll(V... options) {
    List<V> optionList = Arrays.asList(options);

    m_updateLock.lock();
    try {
      if (m_optionsWanted.retainAll(optionList) || m_optionsWanted.addAll(optionList)) {
        m_updateReq = true;
        updateOptions();
      }
    } finally {
      m_updateLock.unlock();
    }
  }

  /**
   * If the chooser needs to be updated to sync with shuffleboard.
   * Updates can be performed by selecting the default option on shuffleboard.
   *
   * @return if the chooser needs to be updated
   */
  public boolean isUpdateReq() {
    m_updateLock.lock();
    try {
      return m_updateReq;
    } finally {
      m_updateLock.unlock();
    }
  }

  /**
   * Returns the selected option, and the default if there is no selection.
   *
   * @return the selected option
   */
  public V getSelected() {
    m_networkLock.lock();
    try {
      return m_linkedOptions.get(m_selectedKey);
    } finally {
      m_networkLock.unlock();
    }
  }

  /**
   * Binds a {@link BiConsumer} to a change in the chooser's selection.
   * The first input is the old option, and the second input is the new option.
   *
   * @param bindTo the consumer to bind to
   */
  public void bindTo(BiConsumer<V, V> bindTo) {m_bindTo = bindTo;}

  @Override
  public void initSendable(NTSendableBuilder builder) {
    builder.setSmartDashboardType("String Chooser");

    IntegerPublisher instancePub = new IntegerTopic(builder.getTopic(INSTANCE)).publish();
    instancePub.set(m_instance);
    builder.addCloseable(instancePub);

    builder.addStringProperty(DEFAULT, () -> m_defaultKey, null);
    builder.addStringArrayProperty(OPTIONS, () -> m_linkedOptions.keySet().toArray(new String[0]), null);

    builder.addStringProperty(ACTIVE,
      () -> {
        m_networkLock.lock();
        try {
          return m_selectedKey;
        } finally {
          m_networkLock.unlock();
        }
      },
      null
    );

    m_networkLock.lock();
    try {
      m_activePubs.add(new StringTopic(builder.getTopic(ACTIVE)).publish());
    } finally {
      m_networkLock.unlock();
    }

    builder.addStringProperty(SELECTED,
      null,
      newSelection -> {
        V oldSelection;

        m_networkLock.lock();
        try {
          oldSelection = m_linkedOptions.get(m_selectedKey);

          m_activePubs.forEach(pub -> pub.set(newSelection));
          m_selectedKey = newSelection;
          if (m_updateReq) {updateOptions();}
        } finally {
          m_networkLock.unlock();
        }

        if (m_bindTo != null) {
          m_bindTo.accept(oldSelection, m_linkedOptions.get(newSelection));
        }
      }
    );
  }

  @Override
  public void close() {
    SendableRegistry.remove(this);
    m_networkLock.lock();
    try {
      m_activePubs.forEach(pub -> pub.close());
    } finally {
      m_networkLock.unlock();
    }
  }
}