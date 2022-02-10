package dev.vgerasimov.templo

import dev.vgerasimov.templo.types.*

import org.scalacheck.Gen
import org.scalacheck.Prop.*

trait TemploTestSuite[A, B](underTest: A => Either[TemploError, B]) extends munit.ScalaCheckSuite:
  export TemploTestSuite.*

  def testGen(name: String)(testCase: (Gen[A], A => B)) = testCase match
    case (gen, expected) =>
      test(name) {
        forAll(gen) { toPass =>
          assertRes(underTest(toPass), expected(toPass))
        }
      }

  def testOne(name: String)(testCase: (A, B)) = testCase match
    case (toPass, expected) => test(name) { assertRes(underTest(toPass), expected) }

  def testGenError(name: String)(testCase: (Gen[A], A => TemploError)) = testCase match
    case (gen, expected) =>
      test(name) {
        forAll(gen) { toPass =>
          assertError(underTest(toPass), expected(toPass))
        }
      }

  def testOneError(name: String)(testCase: (A, TemploError)) = testCase match
    case (toPass, expected) => test(name) { assertError(underTest(toPass), expected) }

  def assertRes(obtained: Either[TemploError, B], expected: B) = obtained match
    case Left(error)   => fail(s"Failed with error:\n$error")
    case Right(actual) => assertEquals(actual, expected)

  def assertError(obtained: Either[TemploError, B], expected: TemploError) = obtained match
    case Left(error)   => assertEquals(error, expected)
    case Right(actual) => fail(s"Succeed with result:\n$actual")

object TemploTestSuite:
  def one[A](value: A): Gen[A] = oneOf(value)
  def oneOf[A](values: A*): Gen[A] = Gen.oneOf(values.toSeq)

  def parseUnsafe(parser: Parser)(s: String): List[Block] = parser(s) match
    case Right(expr) => expr
    case Left(error) => sys.error(s"Unexpected error: $error")
