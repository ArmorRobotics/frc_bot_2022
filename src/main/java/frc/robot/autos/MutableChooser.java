package frc.robot.autos;

import edu.wpi.first.networktables.NTSendable;
import edu.wpi.first.networktables.NTSendableBuilder;
import edu.wpi.first.util.sendable.SendableRegistry;

import edu.wpi.first.networktables.StringTopic;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.networktables.IntegerTopic;
import edu.wpi.first.networktables.IntegerPublisher;

import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

// TODO: Make removal only possible when on or changing to default option, add warning for reset required
/** A {@link edu.wpi.first.wpilibj.smartdashboard.SendableChooser SendableChooser}-like class allowing for the removal of options. */
public class MutableChooser<V extends Enum<V> & AutoSelector.AutoType> implements NTSendable, AutoCloseable {
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

  /** Reentrant lock to stop simultaneous editing. */
  private final ReentrantLock m_lock = new ReentrantLock(true);

  /** A map linking options to their identifiers. */
  private final LinkedHashMap<String, V> linkedOptions = new LinkedHashMap<>();
  /** Default selection. */
  private final String m_default;
  /** Current selection. */
  private String m_selected;

  /** A consumer to be called with the old and new selections when the selection changes. */
  private BiConsumer<V, V> m_bindTo = (t, u) -> {};

  /** List to keep track of publishers. */
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

    m_default = obj.getName();
    m_selected = m_default;
    linkedOptions.put(m_default, obj);
  }

  /**
   * Adds the given option to the chooser if it does not already contain it.
   * Only works if the current selection is the default option.
   *
   * @param obj the option to add
   * @return if the option was successfully added
   */
  public boolean add(V obj) {
    if (!m_selected.equals(m_default)) {
      return false;
    }
    
    m_lock.lock();
    try {
      linkedOptions.put(obj.getName(), obj);
      return true;
    } finally {
      m_lock.unlock();
    }
  }

  /**
   * Removes the given option from the chooser if it is not the default option.
   * Only works if the current selection is the default option.
   *
   * @param obj the option to remove
   * @return if the option was successfully removed
   */
  public boolean remove(V obj) {
    if (obj.getName().equals(m_default) || !m_selected.equals(m_default)) {
      return false;
    }

    m_lock.lock();
    try {
      System.out.print("Options:");
      linkedOptions.keySet().forEach(e -> System.out.print(", " + e));
      System.out.println();
      linkedOptions.remove(obj.getName());
      return true;
    } finally {
      m_lock.unlock();
    }
  }

  /**
   * Returns the selected option, and the default if there is no selection.
   *
   * @return the selected option
   */
  public V getSelected() {
    m_lock.lock();
    try {
      return linkedOptions.get(m_selected);
    } finally {
      m_lock.unlock();
    }
  }

  /**
   * Binds a {@link BiConsumer} to a change in the chooser's selection.
   * The first input is the old option, and the second input is the new option.
   *
   * @param bindTo the consumer to bind to
   */
  public void bindTo(BiConsumer<V, V> bindTo) {
    m_bindTo = bindTo;
  }

  @Override
  public void initSendable(NTSendableBuilder builder) {
    builder.setSmartDashboardType("String Chooser");

    IntegerPublisher instancePub = new IntegerTopic(builder.getTopic(INSTANCE)).publish();
    instancePub.set(m_instance);
    builder.addCloseable(instancePub);

    builder.addStringProperty(DEFAULT, () -> m_default, null);
    builder.addStringArrayProperty(OPTIONS, () -> linkedOptions.keySet().toArray(new String[0]), null);

    builder.addStringProperty(ACTIVE,
      () -> {
        m_lock.lock();
        try {
          System.out.println("Active: " + m_selected);
          return m_selected;
        } finally {
          m_lock.unlock();
        }
      },
      null
    );

    m_lock.lock();
    try {
      m_activePubs.add(new StringTopic(builder.getTopic(ACTIVE)).publish());
    } finally {
      m_lock.unlock();
    }

    builder.addStringProperty(SELECTED,
      null,
      newSelection -> {
        m_lock.lock();
        System.out.println("Started change: " + m_selected + " to " + newSelection);
        try {
          System.out.println("Selected: " + m_selected + " becomes " + newSelection);
          m_selected = newSelection;
          m_bindTo.accept(
            linkedOptions.get(m_selected),
            linkedOptions.get(m_selected = newSelection)
          );
          m_activePubs.forEach(pub -> pub.set(newSelection));
        } finally {
          System.out.println("Ended change to " + newSelection);
          m_lock.unlock();
        }
      }
    );
  }

  @Override
  public void close() {
    SendableRegistry.remove(this);
    m_lock.lock();
    try {
      m_activePubs.forEach(pub -> pub.close());
    } finally {
      m_lock.unlock();
    }
  }
}