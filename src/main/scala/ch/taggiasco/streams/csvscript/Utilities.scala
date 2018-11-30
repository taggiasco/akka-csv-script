package ch.taggiasco.streams.csvscript



object Utilities {
  
  def using[A <: { def close(): Unit }, B](resource: A)(f: A => B): B = {
    try {
        f(resource)
    } finally {
        resource.close()
    }
  }
  
}
