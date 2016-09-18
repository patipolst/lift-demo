package code.snippet

import net.liftweb.util.Helpers._

class ExampleSnippet {
  def render = "#abc" #> "from snippet"
}

class AnotherSnippet {
  // def render = "*" #> "from another snippet"
  def render = "*" #> <span>Welcome to helloworld at
    {new _root_.java.util.Date}</span>
}
