// KT-55828
// IGNORE_BACKEND_K2: NATIVE
fun box() = if(Context.operatingSystemType == Context.Companion.OsType.OTHER) "OK" else "fail"

public class Context
{
        companion object
        {
                public enum class OsType {
                        LINUX,
                        OTHER;
                }

                public val operatingSystemType: OsType
                        get() = OsType.OTHER
        }
}
