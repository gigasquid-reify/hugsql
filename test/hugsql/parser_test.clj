(ns hugsql.parser-test
  (:require [clojure.test :refer :all]
            [hugsql.parser :refer [parse]])
  (:import [clojure.lang ExceptionInfo]))

(deftest parsing

  (testing "empty string"
    (is (thrown? ExceptionInfo (parse ""))))

  (testing "SQL comments"
    (is (= [] (parse "-- an sql comment")))
    (is (= [] (parse "/* a\nmulti-line\ncomment */"))))

  (testing "hugsql header comments"
    (is (= [{:hdr {:name ["test"]}
             :sql []}]
          (parse "-- :name test")))
    (is (= [{:hdr {:doc ["this\nis\n\ndoc"]}
             :sql []}]
          (parse "/* :doc this\nis\n\ndoc\n*/"))))

  (testing "SQL"
    (testing "no hugsql header"
      (is (thrown? ExceptionInfo (parse "select * from emp"))))

    (testing "hugsql header"
      (is (= [{:hdr {:name ["test"]}
               :sql ["select * from emp"]}]
            (parse "-- :name test\nselect * from emp"))))
    
    (testing ":param-name (default value parameter type)"
      (is (= [{:hdr {:name ["test"]}
               :sql ["select * from emp where id = "
                     {:type :v :name :id}]}]
            (parse "-- :name test\nselect * from emp where id = :id"))))

    (testing ":param-type:param-name (explicit parameter type)"
      (is (= [{:hdr {:name ["test"]}
               :sql ["select * from emp where id = "
                     {:type :value :name :id}]}]
            (parse "-- :name test\nselect * from emp where id = :value:id"))))

    (testing "::sometype is a Postgresql cast and not a hugsql param"
      (is (= [{:hdr {:name ["test"]}
               :sql ["select id::text as id-str from emp where id = "
                     {:type :v :name :id}]}]
            (parse "-- :name test\nselect id::text as id-str from emp where id = :id"))))


     (testing "SQL quoted strings (ignored and do not replace params)"
      (is (= [{:hdr {:name ["test"]}
               :sql ["select \"col1\" from emp"]}]
            (parse "-- :name test\nselect \"col1\" from emp")))
      (is (= [{:hdr {:name ["test"]}
               :sql ["select * from emp where \"col1\" = 'my :param is safe'"]}]
             (parse "-- :name test\nselect * from emp where \"col1\" = 'my :param is safe'"))))))
