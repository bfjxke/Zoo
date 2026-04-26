export const NodeType = {
    BASE: 'BASE',
    CENTER: 'CENTER',
    WILDERNESS: 'WILDERNESS'
}

export const Faction = {
    LAWFUL: 'lawful',
    AGGRESSIVE: 'aggressive',
    NEUTRAL: 'neutral',
    NONE: null
}

export const FactionColors = {
    lawful: '#3b82f6',
    aggressive: '#ef4444',
    neutral: '#22c55e',
    none: '#ffd700',
    center: '#ffd700'
}

export const FactionLabels = {
    lawful: '守序',
    aggressive: '激进',
    neutral: '中立'
}

export const NodeTypeIcons = {
    BASE: '🏛',
    CENTER: '⭐',
    WILDERNESS: '🌲'
}

export function getNodeColor(node) {
    if (node.type === 'CENTER') {
        return FactionColors.center
    }
    return FactionColors[node.faction] || FactionColors.none
}

export function getNodeIcon(node) {
    if (node.icon) return node.icon
    
    if (node.type === 'CENTER') {
        return '⭐'
    }
    
    if (node.type === 'WILDERNESS') {
        switch (node.id) {
            case 'F': return '🌲'
            case 'G': return '🌊'
            case 'H': return '⛰'
            default: return '🌲'
        }
    }
    
    if (node.type === 'BASE') {
        switch (node.faction) {
            case 'lawful': return '🏛'
            case 'aggressive': return '⚔️'
            case 'neutral': return '⚖️'
            default: return '🏛'
        }
    }
    
    return '📍'
}

export function getNodeLabel(node) {
    if (node.label && node.label.trim()) {
        return node.label
    }
    
    if (node.type === 'CENTER') {
        return '河流'
    }
    
    if (node.type === 'WILDERNESS') {
        switch (node.id) {
            case 'F': return '森林'
            case 'G': return '河流'
            case 'H': return '山地'
            default: return '野外'
        }
    }
    
    if (node.type === 'BASE') {
        switch (node.faction) {
            case 'lawful': return '守序'
            case 'aggressive': return '激进'
            case 'neutral': return '中立'
            default: return '基地'
        }
    }
    
    return node.id
}
