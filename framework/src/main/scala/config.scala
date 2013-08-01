package bhoot

object Config {
  val maxExpiryDuration = 45

  val itemActive    = 0
  val itemDenied    = 1
  val itemModerated = 2
  val itemExpired   = 3

  val itemStatusDescr = Array(
    """<span class="status active">Active</span>""",
    """<span class="status denied">Denied</span>""",
    """<span class="status moderated">Awaiting approval</span>""",
    """<span class="status expired">Expired</span>""")


  val ourSecretCode = "trunityishowitstarted"
}
