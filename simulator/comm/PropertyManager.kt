package venusbackend.simulator.comm

class PropertyManager() {
    /**
     * @return the port
     */
    /**
     * @param port the port to set
     */
    var port = 9002
    /**
     * @return the hostName
     */
    /**
     * @param hostName the hostName to set
     */
    /**
     * @return the host
     */
    var hostname = "localhost"
        get() = field
        set
    /**
     * @return the startAddress
     */
    /**
     * @param startAddress the startAddress to set
     */
    var startAddress = 0x0000000000000L
    /**
     * @return the width
     */
    /**
     * @param width the width to set
     */
    var width = 640
    /**
     * @return the height
     */
    /**
     * @param height the height to set
     */
    var height = 480
}