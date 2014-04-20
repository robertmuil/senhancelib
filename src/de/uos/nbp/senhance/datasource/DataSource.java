package de.uos.nbp.senhance.datasource;

import de.uos.nbp.senhance.DataLogger;
import android.os.Bundle;
import android.os.Handler;

/**
 * An DataSource is a class that will acquire (in the case of
 * physical devices) or generate (in the case of, for example,
 * simulators) data events. An DataSource needs an {@link DataSink}
 * as a destination for the events.
 * DataSources have a defined set of states according to the state
 * machine documented in the
 * <a href="https://ikw.uni-osnabrueck.de/trac/heartFelt/wiki/Software/Android">wiki</a>,
 * and a set of {@link UIEvent}s which inform the UI of user-relevant occurrences.
 * 
 * DataSources effectively have 3 outputs:
 * <ol>
 *  <li> DataLogger - only one, this is where the data gets sent to be preserved for later.
 *  <li> DataSink(s) - potentially multiple, these modules consume the data as it comes in.
 *  <li> Handler - this is the UI handler that can be sent certain messages relevant for the user.
 * </ol>
 * 
 * @author rmuil@uos.de
 */
public interface DataSource {
	static final int DefaultThreadPriority = DataSink.DefaultThreadPriority - 2;

	/**
	 * These are the coarse-grained states
	 * of the underlying device of the DataSource.
	 */
	enum DeviceState {
		/** underlying generator not connected */
		Disconnected,
		/** in process of initiating connection to event generator */
		Connecting,
		/** connected to the event generator  */
		Connected,
		/** standing by to provide data */
		Ready, 
		/** connected and providing events */
		Transmitting,
		Undefined;
		
		/**
		 * Essentially the inverse of {@link valueOf} -
		 * returns the DeviceState corresponding to the given value.
		 * 
		 * @param ii
		 * @return the DeviceState corresponding, or <i>Undefined</i>.
		 */
		public static DeviceState fromInt(int ii) {
			try {
				return values()[ii];
			} catch (ArrayIndexOutOfBoundsException e) {
				return Undefined;
			}
		}
	}
	
	/**
	 * These event constants are used for communication with the
	 * user interface.
	 * They generally describe device connection events rather than
	 * data events although data events can also be communicated to the
	 * user interface in some circumstances. 
	 * 
	 * They are one-off events (e.g. transitions), not states.
	 */
	enum UIEvent {
		/** underlying event generator changed state */
		StateChange,			
		/** beginning attempt to initiate connection - the Bundle of the message should contain the name and address of the device */
		AttemptingConnection,	
		/** attempt to connect to generator failed */
		ConnectionAttemptFailed,
		/** an established connection came down unexpectedly */
		ConnectionDropped,
		/** an established connection was closed deliberately */
		ConnectionClosed,
		/** data did not arrive in expected time */
		TransmissionTimeout,	
		/** event generator has a low battery */
		LowOnBattery,
		/** a data event occurred of interest to the user (e.g, heart-beat pulse) */
		DataEvent,
		/** a regularly sent message with metrics of the connection. */
		Metrics,
		/** a control event, as sent e.g. from a ControlSource */
		ControlCommand,
		/** undefined */
		Undefined;
		
		public static UIEvent fromInt(int ii) {
			try {
				return values()[ii];
			} catch (ArrayIndexOutOfBoundsException e) {
				return Undefined;
			}
		}
	}
	
	final static String MsgDeviceType = "DeviceType";
	final static String MsgDeviceName = "DeviceName";
	final static String MsgDeviceAddress = "DeviceAddress";


	/**
	 * Attaches an eventLogger to this source. The source should
	 * then prepare the eventlogger appropriately.
	 * 
	 * Without a call to this function, the event logger will not be
	 * attached and the data will not be persisted.
	 * 
	 * @param eventLogger if this is given, it will be used to persist the data
	 */
	void attachLogger(DataLogger eventLogger);
	
	
	/**
	 * This function attaches an event sink to this source. Sinks are
	 *  where the data goes.
	 * 
	 * @param dataSink the DataSink to be attached to this source.
	 */ 
	void attachSink(DataSink dataSink);
	
	/**
	 * Removes a previously attached event sink from this source.
	 * 
	 * @param sinkToDetach
	 */
	void detachSink(DataSink sinkToDetach);
	
	/**
	 * Determines if any sink is attached at all.
	 * @return true if any sink is attached.
	 */
	boolean isSinkAttached();
	
	/**
	 * Determines if a sink <em>of the given type</em>
	 * is attached to the DataSource.
	 * 
	 * @param sink
	 * @return true if a sink of the given type is attached.
	 */
	//boolean isSinkTypeAttached (Class<DataSink> sinkType);

	/**
	 * Attaches a handler (call-back) to the DataSource for asynchronous messages.
	 * 
	 * The handler would typically be that of a user interface.
	 * The types of messages that get sent are those
	 * relevant to the user - things like state changes, metrics, etc.
	 * 
	 * The {@value sourceID} should be generated by the calling class and
	 * the implementation of the DataSource should include this id in
	 * every message sent to the handler. This allows the handler to determine
	 * from where the message comes in case more than one source is used.
	 * As far as I know, the handler structure itself
	 * doesn't have a mechanism to determine message origin and so if there
	 * are multiple sources (which is likely) then messages would be all
	 * mixed up by the user interface.
	 * 
	 * All messages back to the handler are expected to contain:
	 * <ol>
	 *  <li> what - (int) value referencing a {@link UIEvent}.
	 *  <li> arg1 - (int) the sourceID that was passed to this function
	 *  <li> arg2 - (int) <i>[optional]</i> an argument for this particular UIEvent
	 *  <li> bundle - (Bundle) <i>[optional]</i> extended arguments for this UIEvent
	 * </ol> 
	 * 
	 * @param handler the Handler to send messages back to the UI Activity.
	 * @param sourceID this identifies this event source to the receiving handler.
	 */
	void attachUIHandler (Handler handler, int sourceID);
	
	/**
	 * Detaches the handler from this event source.
	 * 
	 * @param handler
	 */
	void detachUIHandler (Handler handler);
	
	/**
	 * Returns the sourceID as given in {@link attachUIHandler}.
	 * @return sourceID
	 */
	int getSourceID ();
	
	
	/**
	 * Initiates a connection to the remote device specified by address.
	 * 
	 * Auto-start should be disabled in this case.
	 *   
	 * @param address the address of the underlying event generator device. The form of
	 *   this depends on the particular implementation (is typically a MAC address).
	 */
	void connect(String address);
	
	/**
	 * Same as connect(address) but allows setting of auto-start: which means
	 * that the data source will begin providing data immediately a connection
	 * is successfully established without waiting for a call to
	 * {@link startEvents}.
	 *  
	 * @param address
	 * @param autoStart
	 */
	void connect(String address, boolean autoStart);

	void startEvents();
	void configure(Bundle parameters);
	boolean needBluetooth();
	
	/**
	 * Return a string identifying the type of device.
	 */
	public String getTypeString();
	
	/**
	 * Returns a string that will identify this device,
	 * in a human readable form, including the type.
	 * 
	 * Format should be:
	 * <ul>
	 *  <li> If name is relevant and available: "<tt>Type_Name</tt>"
	 *  <li> If name is relevant but unavailable: "<tt>Type@Address</tt>"
	 *  <li> If name is irrelevant: "<tt>Type</tt>"
	 *  <li> e.g.: "<tt>ECG_30716</tt>"
	 *  <li> e.g.: "<tt>ECG@00:A0:96:2E:55:C8</tt>"
	 *  <li> e.g.: "<tt>ExPC_saturn</tt>"
	 *  <li> e.g.: "<tt>DummyECG</tt>"
	 * </ul>
	 * 
	 */
	public String getDeviceIDString();
	
	/**
	 * The string used to filter devices when searching.
	 * 
	 * @return
	 */
	String getDeviceNameFilterString();
	
	/**
	 * Returns the connection state of the underlying device.
	 * @see https://ikw.uni-osnabrueck.de/trac/heartFelt/wiki/Software/Android
	 * @return device state
	 */
	DeviceState getSourceDeviceState();
	
	/**
	 * This should stop data being generated by the source.
	 */
	void stopEvents();
	
	/**
	 * This should stop data being generated, and then disconnect from the
	 * source.
	 */
	void disconnect();
	
	/**
	 * The baseline can be considered the average value
	 * of the event source.
	 * 
	 * In the case of ECG or BPM, the natural choice is for
	 * the baseline to be the RR interval, in the unit beats-per-minute.
	 * 
	 * In the case of dummy sources, setting this value allows control of the
	 * data being generated.
	 * 
	 * In the case of real data sources, setting this value
	 * does nothing. Be rather humorous if it did.
	 * 
	 * @param newBaseline
	 */
	void setBaseline(float newBaseline);
	float getBaseline();
	
	/**
	 * The instantaneous value should be the typical quantification of the data
	 * of this source. For an ECG or pulse device, this is BPM by convention.
	 * @return instantaneous value of data
	 */
	float getCurrent();

}