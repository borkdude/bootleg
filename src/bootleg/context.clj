(ns bootleg.context)

(def ^:dynamic *path* ".")

(defmacro with-path [path & body]
  `(binding [*path* ~path]
     ~@body))

#_ (macroexpand-1 '(with-path "foo/bar" a b c))

(def ^:dynamic *colour* false)

(defmacro with-colour [colour? & body]
  `(binding [*colour* ~colour?]
     ~@body))
