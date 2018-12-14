package ch.taggiasco.streams.csvscript



object Utilities {
  
  def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B = {
    try {
        f(resource)
    } finally {
        resource.close()
    }
  }
  
  
  implicit class When[A](a: A) {
    def when(f: A => Boolean)(g: A => A) = if (f(a)) g(a) else a
    def when(f:   => Boolean)(g: A => A) = if (f) g(a) else a
  }
  
}
