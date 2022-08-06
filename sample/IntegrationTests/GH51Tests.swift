//
//  GH51Tests.swift
//  Sample
//
//  Created by Rick Clephas on 26/03/2022.
//

import XCTest
import KMPNativeCoroutinesCore
import NativeCoroutinesSampleShared

class GH51Tests: XCTestCase {
    
    func testInterfaceCImplFoo() {
        let interfaceC = InterfaceCImpl()
        let valueExpectation = expectation(description: "Waiting for value")
        let sendValue = randomInt()
        _ = interfaceC.fooNative(value: sendValue)({ value, next, _ in
            XCTAssertEqual(value.int32Value, sendValue, "Received incorrect value")
            valueExpectation.fulfill()
            return next()
        }, { _, unit in unit }, { _, unit in unit })
        wait(for: [valueExpectation], timeout: 2)
    }
    
    func testInterfaceCImplBar() {
        let interfaceC = InterfaceCImpl()
        let valueExpectation = expectation(description: "Waiting for value")
        _ = interfaceC.barNative({ value, next, _ in
            XCTAssertEqual(value, 1, "Received incorrect value")
            valueExpectation.fulfill()
            return next()
        }, { _, unit in unit }, { _, unit in unit })
        wait(for: [valueExpectation], timeout: 2)
    }
    
    func testClassCImplFoo() {
        let classC = ClassCImpl()
        let valueExpectation = expectation(description: "Waiting for value")
        let sendValue = randomInt()
        _ = classC.fooNative(value: sendValue)({ value, next, _ in
            XCTAssertEqual(value.int32Value, sendValue, "Received incorrect value")
            valueExpectation.fulfill()
            return next()
        }, { _, unit in unit }, { _, unit in unit })
        wait(for: [valueExpectation], timeout: 2)
    }
    
    func testClassCImplBar() {
        let classC = ClassCImpl()
        let valueExpectation = expectation(description: "Waiting for value")
        _ = classC.barNative({ value, next, _ in
            XCTAssertEqual(value, 1, "Received incorrect value")
            valueExpectation.fulfill()
            return next()
        }, { _, unit in unit }, { _, unit in unit })
        wait(for: [valueExpectation], timeout: 2)
    }
}
