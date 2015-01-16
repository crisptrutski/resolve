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

(defn bump-expanded [[maj min patch pre :as expanded]]
  (cond (not (empty? pre))
        [maj min patch ""]

        (> maj 0)
        [(inc maj) 0 0 ""]

        (> min 0)
        [0 (inc min) 0 ""]

        :else
        [0 0 (inc patch) ""]))

(def bump (comp print-version bump-expanded expand-version))

(defn match [versions & [version]]
  ;; TODO: rather assert versions are sorted, and skip sort and last
  (if version
    (if (re-find #"^\^" version)
      ;; semantic upgrade, aka caret range
      (let [min (expand-version (.substring version 1))
            max (bump-expanded min)
            top (last (sort (filter (fn [v]
                                      (and (pos? (compare v min))
                                           (neg? (compare v max))))
                                    versions)))]
        (if top (print-version top)))
      ;; exact
      (let [v (first (filter #{(expand-version version)} versions))]
        (if v (print-version v))))
    ;; use latest
    (if-let [max (last (sort versions))]
      (print-version max))))
