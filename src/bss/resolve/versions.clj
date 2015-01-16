(ns bss.resolve.versions
  (:require [clojure.string :as str]))

(defn expand-version [ver-str]
  (let [[base metadata]     (str/split ver-str #"\+" 2)
        [maj min patch pre] (str/split base #"\.|-" 4)]
    ;; ignore metadata
    (mapv #(%2 %1)
          [maj min patch pre]
          [#(if % (Long/parseLong %) 0)
           #(if % (Long/parseLong %) 0)
           #(if % (Long/parseLong %) 0)
           #(or % "")])))

(defn print-version [expanded]
  (let [pre (first (drop 3 expanded))
        [maj min patch] (map #(or % 0) (take 3 expanded))]
    (if (empty? pre)
      (format "%s.%s.%s" maj min patch)
      (format "%s.%s.%s-%s" maj min patch pre))))

(def normalize (comp print-version expand-version))

(defn match [versions & [version]]
  (if version
    (if (re-find #"^ " version)
      ;; semantic upgrade
      :todo
      ;; exact
      (let [v (first (filter #{(expand-version version)} versions))]
        (if v (print-version v))))
    ;; use latest
    ;; TODO: higher up just ensure always sorted
    (if-let [max (last (sort versions))]
      (print-version max))))
