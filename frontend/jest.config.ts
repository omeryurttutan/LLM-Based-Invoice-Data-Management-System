import type { Config } from 'jest'
import nextJest from 'next/jest.js'

const createJestConfig = nextJest({
    dir: './',
})

const config: Config = {
    coverageProvider: 'v8',
    testEnvironment: 'jsdom',
    testEnvironmentOptions: {
        customExportConditions: [''],
    },
    setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
    moduleNameMapper: {
        '^@/(.*)$': '<rootDir>/src/$1',
        '^msw/node$': '<rootDir>/node_modules/msw/lib/node/index.js',
    },
    transformIgnorePatterns: [
        '/node_modules/(?!(msw|@mswjs)/)'
    ],
    testPathIgnorePatterns: [
        '<rootDir>/node_modules/',
        '<rootDir>/e2e/'
    ]
}

export default createJestConfig(config)
