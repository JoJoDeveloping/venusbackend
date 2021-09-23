package venusbackend.simulator

interface Memory {
    fun removeByte(address: Number)
    fun loadByte(address: Number): Int
    fun loadHalfWord(addr: Number): Int
    fun loadWord(addr: Number): Int
    fun loadLong(addr: Number): Long
    fun storeByte(addr: Number, value: Number)
    fun storeHalfWord(addr: Number, value: Number)
    fun storeWord(addr: Number, value: Number)
    fun storeLong(addr: Number, value: Number)
}