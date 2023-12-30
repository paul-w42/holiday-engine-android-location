package net.nwnetsolutions.holidayengine;

/**
 * This Spinner presents to the user a selection of Engines to transmit.
 */
public class SpinnerSelectEngineEntry {

    public String id;
    public String name;
    public boolean transmitting;

    public SpinnerSelectEngineEntry(String id, String name) {
        this.id = id;
        this.name = name;

        /***********************************************************************************
         * When we start to transmit, this will tell us what engine, and whether transmitting
         * or not.  This way we can tell the server to 'stop' on this engine if user attempts
         * to transmit another.
         */
        this.transmitting = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getTransmitting() {
        return transmitting;
    }

    public void setTransmitting(boolean transmitting) {
        this.transmitting = transmitting;
    }


    @Override
    public String toString() {
        return name;
    }

}
